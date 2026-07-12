package com.parazit.panel.integration.payment.manual.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptAlreadySubmittedException;
import com.parazit.panel.application.payment.manual.receipt.command.SubmitManualPaymentReceiptCommand;
import com.parazit.panel.application.payment.manual.receipt.result.SubmitManualPaymentReceiptResult;
import com.parazit.panel.application.port.in.payment.manual.receipt.SubmitManualPaymentReceiptUseCase;
import com.parazit.panel.application.port.out.payment.receipt.PaymentReceiptContent;
import com.parazit.panel.application.port.out.payment.receipt.PaymentReceiptStorage;
import com.parazit.panel.application.port.out.payment.receipt.StorePaymentReceiptCommand;
import com.parazit.panel.application.port.out.payment.receipt.StoredPaymentReceipt;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.manual.BankCardNumber;
import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.ManualPaymentDestination;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "app.payment.receipt-storage.enabled=true",
        "app.payment.receipt-storage.provider=local",
        "app.payment.receipt-storage.local-root=/tmp/panel-task30-concurrent-receipts",
        "app.payment.receipt-storage.max-file-size-bytes=5242880",
        "app.payment.receipt-storage.allow-pdf=true"
})
@Import({
        MutableClockTestConfiguration.class,
        ConcurrentManualPaymentReceiptSubmissionIntegrationTest.BlockingStorageConfiguration.class
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ConcurrentManualPaymentReceiptSubmissionIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");
    private static final ManualPaymentDestination DESTINATION = new ManualPaymentDestination(
            "PRIMARY_CARD",
            "Example Bank",
            "Example Holder",
            BankCardNumber.parse("6037990000000014"),
            true,
            0
    );

    private final SubmitManualPaymentReceiptUseCase submitUseCase;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ManualCardPaymentInstructionRepository instructionRepository;
    private final ManualPaymentReceiptRepository receiptRepository;
    private final JdbcTemplate jdbcTemplate;
    private final MutableTestClock clock;
    private final BlockingPaymentReceiptStorage storage;

    ConcurrentManualPaymentReceiptSubmissionIntegrationTest(
            SubmitManualPaymentReceiptUseCase submitUseCase,
            UserRepository userRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            ManualCardPaymentInstructionRepository instructionRepository,
            ManualPaymentReceiptRepository receiptRepository,
            JdbcTemplate jdbcTemplate,
            Clock clock,
            PaymentReceiptStorage storage
    ) {
        this.submitUseCase = submitUseCase;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.instructionRepository = instructionRepository;
        this.receiptRepository = receiptRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = (MutableTestClock) clock;
        this.storage = (BlockingPaymentReceiptStorage) storage;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPaymentTables(jdbcTemplate);
        clock.setInstant(NOW);
        storage.reset();
    }

    @Test
    void concurrentSubmissionsForSameInstructionLeaveOneQueuedReceiptAndOneConflict() throws Exception {
        Payment payment = payment(906001L);
        ManualCardPaymentInstruction instruction = instruction(payment, 1_638L);
        byte[] png = pngBytes();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<SubmitManualPaymentReceiptResult> first = executor.submit(() -> submitUseCase.submit(command(
                payment.getId(),
                UUID.randomUUID(),
                instruction.getPayableAmount(),
                png
        )));
        assertThat(storage.awaitFirstStore()).isTrue();

        Future<?> second = executor.submit(() -> submitUseCase.submit(command(
                payment.getId(),
                UUID.randomUUID(),
                instruction.getPayableAmount(),
                png
        )));

        assertThatThrownBy(second::get)
                .hasCauseInstanceOf(ManualPaymentReceiptAlreadySubmittedException.class);
        storage.releaseFirstStore();

        SubmitManualPaymentReceiptResult result = first.get(5, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(result.receipt().receiptStatus()).isEqualTo(ManualPaymentReceiptStatus.QUEUED_FOR_REVIEW);
        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.WAITING_FOR_REVIEW);
        assertThat(receiptRepository.findAllByPaymentIdOrderBySubmittedAtDesc(payment.getId()))
                .singleElement()
                .satisfies(receipt -> assertThat(receipt.getStatus()).isEqualTo(ManualPaymentReceiptStatus.QUEUED_FOR_REVIEW));
        assertThat(storage.storeCount()).isEqualTo(1);
    }

    private SubmitManualPaymentReceiptCommand command(UUID paymentId, UUID requestId, long claimedAmount, byte[] bytes) {
        return new SubmitManualPaymentReceiptCommand(
                requestId,
                906001L,
                paymentId,
                "receipt.png",
                "image/png",
                bytes.length,
                claimedAmount,
                "TRK-1",
                "1234",
                NOW.minusSeconds(60),
                "synthetic",
                () -> new ByteArrayInputStream(bytes)
        );
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

    @TestConfiguration
    static class BlockingStorageConfiguration {

        @Bean
        @Primary
        PaymentReceiptStorage blockingPaymentReceiptStorage() {
            return new BlockingPaymentReceiptStorage();
        }
    }

    static final class BlockingPaymentReceiptStorage implements PaymentReceiptStorage {

        private CountDownLatch firstStoreEntered = new CountDownLatch(1);
        private CountDownLatch releaseFirstStore = new CountDownLatch(1);
        private final AtomicInteger storeCount = new AtomicInteger();

        @Override
        public StoredPaymentReceipt store(StorePaymentReceiptCommand command) {
            int count = storeCount.incrementAndGet();
            if (count == 1) {
                firstStoreEntered.countDown();
                try {
                    if (!releaseFirstStore.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to release first store");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while storing receipt", exception);
                }
            }
            return new StoredPaymentReceipt(
                    "test",
                    "manual-receipts/test/" + command.receiptId() + "/" + command.sanitizedFilename(),
                    command.sanitizedFilename(),
                    command.detectedContentType(),
                    command.fileSizeBytes(),
                    command.fileSha256()
            );
        }

        @Override
        public PaymentReceiptContent load(String storageKey) {
            throw new UnsupportedOperationException("not needed");
        }

        @Override
        public void delete(String storageKey) {
        }

        @Override
        public boolean exists(String storageKey) {
            return true;
        }

        boolean awaitFirstStore() throws InterruptedException {
            return firstStoreEntered.await(5, TimeUnit.SECONDS);
        }

        void releaseFirstStore() {
            releaseFirstStore.countDown();
        }

        int storeCount() {
            return storeCount.get();
        }

        void reset() {
            firstStoreEntered = new CountDownLatch(1);
            releaseFirstStore = new CountDownLatch(1);
            storeCount.set(0);
        }
    }
}
