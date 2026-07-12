package com.parazit.panel.integration.payment.manual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parazit.panel.application.port.out.payment.manual.ManualPaymentSuffixGenerator;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.manual.ManualPaymentInstructionStatus;
import com.parazit.panel.domain.payment.manual.repository.ManualCardPaymentInstructionRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import java.time.Instant;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test",
        "app.payment.manual-card.enabled=true",
        "app.payment.manual-card.bank-name=Example Bank",
        "app.payment.manual-card.card-holder-name=Example Holder",
        "app.payment.manual-card.card-number=6037990000000014",
        "app.payment.manual-card.minimum-suffix=101",
        "app.payment.manual-card.maximum-suffix=4999",
        "app.payment.manual-card.max-generation-attempts=5",
        "app.payment.manual-card.reissue-cooldown=PT0.001S"
})
@AutoConfigureMockMvc
@Import({MutableClockTestConfiguration.class, ManualCardPaymentFlowIntegrationTest.FakeSuffixConfiguration.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ManualCardPaymentFlowIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ManualCardPaymentInstructionRepository instructionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final FakeManualPaymentSuffixGenerator suffixGenerator;

    ManualCardPaymentFlowIntegrationTest(
            MockMvc mockMvc,
            UserRepository userRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            ManualCardPaymentInstructionRepository instructionRepository,
            JdbcTemplate jdbcTemplate,
            ManualPaymentSuffixGenerator suffixGenerator
    ) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.instructionRepository = instructionRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.suffixGenerator = (FakeManualPaymentSuffixGenerator) suffixGenerator;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPaymentTables(jdbcTemplate);
        suffixGenerator.reset();
    }

    @Test
    void initializesReturnsCurrentCancelsAndDoesNotApprovePayment() throws Exception {
        Payment payment = payment(902001L);
        UUID requestId = UUID.randomUUID();
        suffixGenerator.add(1_638L);

        mockMvc.perform(post("/internal/payments/{paymentId}/manual-card/initialize", payment.getId())
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "instructionRequestId": "%s",
                                  "telegramUserId": 902001
                                }
                                """.formatted(requestId)))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("6037990000000014")))
                .andExpect(jsonPath("$.paymentStatus").value("WAITING_FOR_PAYMENT"))
                .andExpect(jsonPath("$.instructionStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.baseAmount").value(100_000))
                .andExpect(jsonPath("$.uniqueSuffixAmount").value(1_638))
                .andExpect(jsonPath("$.payableAmount").value(101_638))
                .andExpect(jsonPath("$.cardNumberMasked").value("6037-****-****-0014"))
                .andExpect(jsonPath("$.newlyInitialized").value(true));

        mockMvc.perform(post("/internal/payments/{paymentId}/manual-card/initialize", payment.getId())
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "instructionRequestId": "%s",
                                  "telegramUserId": 902001
                                }
                                """.formatted(requestId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payableAmount").value(101_638))
                .andExpect(jsonPath("$.newlyInitialized").value(false));

        mockMvc.perform(get("/internal/payments/{paymentId}/manual-card", payment.getId())
                        .with(httpBasic("test", "test"))
                        .param("telegramUserId", "902001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payableAmount").value(101_638));

        mockMvc.perform(post("/internal/payments/{paymentId}/manual-card/cancel", payment.getId())
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "telegramUserId": 902001
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instructionStatus").value("CANCELLED"))
                .andExpect(content().string(not(containsString("stackTrace"))));

        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.WAITING_FOR_PAYMENT);
        assertThat(instructionRepository.findAllByPaymentIdOrderByCreatedAtDesc(payment.getId()))
                .hasSize(1)
                .allSatisfy(instruction -> assertThat(instruction.getStatus())
                        .isEqualTo(ManualPaymentInstructionStatus.CANCELLED));
    }

    @Test
    void activeInstructionIsReusedAndAmountCollisionRetries() throws Exception {
        Payment first = payment(902002L);
        suffixGenerator.add(101L);
        mockMvc.perform(post("/internal/payments/{paymentId}/manual-card/initialize", first.getId())
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instructionRequestId":"%s","telegramUserId":902002}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payableAmount").value(100_101));

        mockMvc.perform(post("/internal/payments/{paymentId}/manual-card/initialize", first.getId())
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instructionRequestId":"%s","telegramUserId":902002}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payableAmount").value(100_101));

        Payment second = payment(902003L);
        suffixGenerator.add(101L, 102L);
        mockMvc.perform(post("/internal/payments/{paymentId}/manual-card/initialize", second.getId())
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instructionRequestId":"%s","telegramUserId":902003}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payableAmount").value(100_102));
    }

    private Payment payment(long telegramUserId) {
        User user = userRepository.save(User.create(telegramUserId, null, "Pay", null, UserLanguage.EN, NOW));
        Order order = orderRepository.save(Order.create(user.getId(), 100_000L, "IRT"));
        return paymentRepository.save(Payment.create(
                order.getId(),
                user.getId(),
                PaymentMethod.CARD_TO_CARD,
                100_000L,
                100_000L,
                "IRT",
                NOW.plusSeconds(1800)
        ));
    }

    @TestConfiguration
    static class FakeSuffixConfiguration {

        @Bean
        @Primary
        ManualPaymentSuffixGenerator fakeManualPaymentSuffixGenerator() {
            return new FakeManualPaymentSuffixGenerator();
        }
    }

    static final class FakeManualPaymentSuffixGenerator implements ManualPaymentSuffixGenerator {

        private final Queue<Long> values = new ConcurrentLinkedQueue<>();

        @Override
        public long generate(long minimumInclusive, long maximumInclusive) {
            Long value = values.poll();
            return value == null ? minimumInclusive : value;
        }

        void add(Long... suffixes) {
            values.addAll(java.util.List.of(suffixes));
        }

        void reset() {
            values.clear();
        }
    }
}
