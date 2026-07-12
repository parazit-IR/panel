package com.parazit.panel.integration.payment.manual.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.manual.BankCardNumber;
import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.ManualPaymentDestination;
import com.parazit.panel.domain.payment.manual.ManualPaymentInstructionStatus;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceiptStatus;
import com.parazit.panel.domain.payment.manual.receipt.repository.ManualPaymentReceiptRepository;
import com.parazit.panel.domain.payment.manual.repository.ManualCardPaymentInstructionRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.MutableTestClock;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test",
        "app.payment.receipt-storage.enabled=true",
        "app.payment.receipt-storage.provider=local",
        "app.payment.receipt-storage.local-root=/tmp/panel-task30-receipts",
        "app.payment.receipt-storage.max-file-size-bytes=5242880",
        "app.payment.receipt-storage.allow-pdf=true",
        "spring.servlet.multipart.max-file-size=5MB",
        "spring.servlet.multipart.max-request-size=6MB"
})
@AutoConfigureMockMvc
@Import(MutableClockTestConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ManualPaymentReceiptFlowIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");
    private static final Path STORAGE_ROOT = Path.of("/tmp/panel-task30-receipts");
    private static final ManualPaymentDestination DESTINATION = new ManualPaymentDestination(
            "PRIMARY_CARD",
            "Example Bank",
            "Example Holder",
            BankCardNumber.parse("6037990000000014"),
            true,
            0
    );

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ManualCardPaymentInstructionRepository instructionRepository;
    private final ManualPaymentReceiptRepository receiptRepository;
    private final JdbcTemplate jdbcTemplate;
    private final MutableTestClock clock;

    ManualPaymentReceiptFlowIntegrationTest(
            MockMvc mockMvc,
            UserRepository userRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            ManualCardPaymentInstructionRepository instructionRepository,
            ManualPaymentReceiptRepository receiptRepository,
            JdbcTemplate jdbcTemplate,
            Clock clock
    ) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.instructionRepository = instructionRepository;
        this.receiptRepository = receiptRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = (MutableTestClock) clock;
    }

    @BeforeEach
    void setUp() throws Exception {
        DatabaseCleaner.cleanPaymentTables(jdbcTemplate);
        deleteRecursively(STORAGE_ROOT);
        Files.createDirectories(STORAGE_ROOT);
        clock.setInstant(NOW);
    }

    @Test
    void uploadsReceiptQueuesReviewAndStreamsContentSafely() throws Exception {
        Payment payment = payment(905001L);
        ManualCardPaymentInstruction instruction = instruction(payment, 1_638L);
        UUID requestId = UUID.randomUUID();
        byte[] png = pngBytes();

        mockMvc.perform(receiptMultipart(payment.getId(), requestId, 905001L, instruction.getPayableAmount(), png)
                        .with(httpBasic("test", "test"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("WAITING_FOR_REVIEW"))
                .andExpect(jsonPath("$.instructionStatus").value("RECEIPT_PENDING"))
                .andExpect(jsonPath("$.receiptStatus").value("QUEUED_FOR_REVIEW"))
                .andExpect(jsonPath("$.claimedAmount").value(101_638))
                .andExpect(jsonPath("$.detectedContentType").value("image/png"))
                .andExpect(jsonPath("$.fileSha256Prefix").exists())
                .andExpect(jsonPath("$.newlySubmitted").value(true))
                .andExpect(jsonPath("$.storageKey").doesNotExist());

        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.WAITING_FOR_REVIEW);
        assertThat(instructionRepository.findById(instruction.getId()).orElseThrow().getStatus())
                .isEqualTo(ManualPaymentInstructionStatus.RECEIPT_PENDING);
        assertThat(receiptRepository.findAllByPaymentIdOrderBySubmittedAtDesc(payment.getId()))
                .singleElement()
                .satisfies(receipt -> {
                    assertThat(receipt.getStatus()).isEqualTo(ManualPaymentReceiptStatus.QUEUED_FOR_REVIEW);
                    assertThat(receipt.getStorageKey()).isNotBlank();
                    assertThat(receipt.isDuplicateHashDetected()).isFalse();
                });
        assertThat(storedFileCount()).isEqualTo(1);

        mockMvc.perform(receiptMultipart(payment.getId(), requestId, 905001L, instruction.getPayableAmount(), png)
                        .with(httpBasic("test", "test"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newlySubmitted").value(false));
        assertThat(storedFileCount()).isEqualTo(1);

        UUID receiptId = receiptRepository.findAllByPaymentIdOrderBySubmittedAtDesc(payment.getId()).getFirst().getId();
        mockMvc.perform(get("/internal/payments/{paymentId}/manual-card/receipt", payment.getId())
                        .with(httpBasic("test", "test"))
                        .param("telegramUserId", "905001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptId").value(receiptId.toString()))
                .andExpect(jsonPath("$.storageKey").doesNotExist());

        mockMvc.perform(get("/internal/admin/manual-payments/review-queue")
                        .with(httpBasic("test", "test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].receiptId").value(receiptId.toString()))
                .andExpect(jsonPath("$[0].claimedAmount").value(101_638))
                .andExpect(jsonPath("$[0].receiptStatus").value("QUEUED_FOR_REVIEW"))
                .andExpect(jsonPath("$[0].storageKey").doesNotExist());

        MvcResult async = mockMvc.perform(get("/internal/admin/manual-payments/receipts/{receiptId}/content", receiptId)
                        .with(httpBasic("test", "test")))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(async))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("image/png")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, not(containsString("manual-receipts"))));
    }

    @Test
    void rejectsAmountMismatchBeforeStoringFile() throws Exception {
        Payment payment = payment(905002L);
        instruction(payment, 101L);

        mockMvc.perform(receiptMultipart(payment.getId(), UUID.randomUUID(), 905002L, 99_999L, pngBytes())
                        .with(httpBasic("test", "test"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Claimed amount")))
                .andExpect(jsonPath("$.message").value(not(containsString("603799"))));

        assertThat(storedFileCount()).isZero();
        assertThat(receiptRepository.findAllByPaymentIdOrderBySubmittedAtDesc(payment.getId())).isEmpty();
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder receiptMultipart(
            UUID paymentId,
            UUID requestId,
            long telegramUserId,
            long claimedAmount,
            byte[] content
    ) {
        return multipart("/internal/payments/{paymentId}/manual-card/receipt", paymentId)
                .file(new MockMultipartFile("receiptRequestId", "", MediaType.TEXT_PLAIN_VALUE, requestId.toString().getBytes(StandardCharsets.UTF_8)))
                .file(new MockMultipartFile("telegramUserId", "", MediaType.TEXT_PLAIN_VALUE, Long.toString(telegramUserId).getBytes(StandardCharsets.UTF_8)))
                .file(new MockMultipartFile("claimedAmount", "", MediaType.TEXT_PLAIN_VALUE, Long.toString(claimedAmount).getBytes(StandardCharsets.UTF_8)))
                .file(new MockMultipartFile("claimedTrackingNumber", "", MediaType.TEXT_PLAIN_VALUE, "TRK-1".getBytes(StandardCharsets.UTF_8)))
                .file(new MockMultipartFile("claimedSenderCardLastFour", "", MediaType.TEXT_PLAIN_VALUE, "1234".getBytes(StandardCharsets.UTF_8)))
                .file(new MockMultipartFile("claimedPaidAt", "", MediaType.TEXT_PLAIN_VALUE, NOW.minusSeconds(60).toString().getBytes(StandardCharsets.UTF_8)))
                .file(new MockMultipartFile("userNote", "", MediaType.TEXT_PLAIN_VALUE, "synthetic upload".getBytes(StandardCharsets.UTF_8)))
                .file(new MockMultipartFile("file", "receipt.png", MediaType.IMAGE_PNG_VALUE, content));
    }

    private Payment payment(long telegramUserId) {
        User user = userRepository.save(User.create(telegramUserId, null, "Pay", null, UserLanguage.EN, NOW));
        Order order = orderRepository.save(Order.create(user.getId(), 100_000L, "IRT"));
        Payment payment = Payment.create(
                order.getId(),
                user.getId(),
                PaymentMethod.CARD_TO_CARD,
                100_000L,
                100_000L,
                "IRT",
                NOW.plusSeconds(1800)
        );
        payment.markWaitingForPayment();
        return paymentRepository.save(payment);
    }

    private ManualCardPaymentInstruction instruction(Payment payment, long suffix) {
        ManualCardPaymentInstruction instruction = ManualCardPaymentInstruction.create(
                payment.getId(),
                UUID.randomUUID(),
                payment.getPayableAmount(),
                suffix,
                payment.getCurrency(),
                DESTINATION,
                NOW,
                Duration.ofMinutes(30)
        );
        instruction.activate();
        return instructionRepository.save(instruction);
    }

    private static byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static long storedFileCount() throws Exception {
        if (!Files.exists(STORAGE_ROOT)) {
            return 0;
        }
        try (java.util.stream.Stream<Path> stream = Files.walk(STORAGE_ROOT)) {
            return stream.filter(Files::isRegularFile).count();
        }
    }

    private static void deleteRecursively(Path root) throws Exception {
        if (!Files.exists(root)) {
            return;
        }
        try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
        }
    }
}
