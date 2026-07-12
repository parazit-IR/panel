package com.parazit.panel.integration.payment.zarinpal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalCreateRequest;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalCreateResponse;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalVerifyRequest;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalVerifyResponse;
import com.parazit.panel.application.port.out.payment.zarinpal.ZarinpalGatewayClient;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalAttemptStatus;
import com.parazit.panel.domain.payment.zarinpal.repository.ZarinpalPaymentAttemptRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
        "app.payment.zarinpal.enabled=true",
        "app.payment.zarinpal.merchant-id=test-merchant",
        "app.payment.zarinpal.success-redirect-url=https://example.test/success",
        "app.payment.zarinpal.failure-redirect-url=https://example.test/failed",
        "app.payment.zarinpal.cancel-redirect-url=https://example.test/cancelled"
})
@AutoConfigureMockMvc
@Import({MutableClockTestConfiguration.class, ZarinpalPaymentFlowIntegrationTest.FakeGatewayConfiguration.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ZarinpalPaymentFlowIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");
    private static final String AUTHORITY = "A000000000000000000000000000123456";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ZarinpalPaymentAttemptRepository attemptRepository;
    private final JdbcTemplate jdbcTemplate;
    private final FakeZarinpalGatewayClient gatewayClient;

    ZarinpalPaymentFlowIntegrationTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            ZarinpalPaymentAttemptRepository attemptRepository,
            JdbcTemplate jdbcTemplate,
            ZarinpalGatewayClient gatewayClient
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.attemptRepository = attemptRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.gatewayClient = (FakeZarinpalGatewayClient) gatewayClient;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPaymentTables(jdbcTemplate);
        gatewayClient.reset();
    }

    @Test
    void initializesRedirectsVerifiesAndReplaysCallbackIdempotently() throws Exception {
        Payment payment = payment(101_001L);
        UUID requestId = UUID.randomUUID();

        String body = mockMvc.perform(post("/internal/payments/{paymentId}/zarinpal/initialize", payment.getId())
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestId": "%s",
                                  "telegramUserId": 101001,
                                  "description": "VPN plan purchase"
                                }
                                """.formatted(requestId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(payment.getId().toString()))
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.paymentStatus").value("WAITING_FOR_PAYMENT"))
                .andExpect(jsonPath("$.attemptStatus").value("REDIRECT_READY"))
                .andExpect(jsonPath("$.paymentUrl").value(containsString(AUTHORITY)))
                .andExpect(jsonPath("$.merchantId").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        UUID attemptId = UUID.fromString(json.get("attemptId").asText());
        assertThat(attemptRepository.findById(attemptId).orElseThrow().getGatewayAmount()).isEqualTo(100_000L);
        assertThat(gatewayClient.createCalls.get()).isEqualTo(1);

        mockMvc.perform(get("/api/payments/zarinpal/callback")
                        .param("Authority", AUTHORITY)
                        .param("Status", "OK"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("https://example.test/success?result=success")))
                .andExpect(header().string("Location", not(containsString(AUTHORITY))));

        Payment approved = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(approved.getGatewayTransactionId()).isEqualTo("987654321");
        assertThat(attemptRepository.findById(attemptId).orElseThrow().getStatus()).isEqualTo(ZarinpalAttemptStatus.VERIFIED);
        assertThat(gatewayClient.verifyCalls.get()).isEqualTo(1);

        mockMvc.perform(get("/api/payments/zarinpal/callback")
                        .param("Authority", AUTHORITY)
                        .param("Status", "OK"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("result=success")));
        assertThat(gatewayClient.verifyCalls.get()).isEqualTo(1);
    }

    @Test
    void cancellationDoesNotCallVerifyAndDoesNotProvisionAnything() throws Exception {
        Payment payment = payment(101_002L);
        UUID requestId = UUID.randomUUID();
        mockMvc.perform(post("/internal/payments/{paymentId}/zarinpal/initialize", payment.getId())
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestId": "%s",
                                  "telegramUserId": 101002,
                                  "description": "VPN plan purchase"
                                }
                                """.formatted(requestId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/payments/zarinpal/callback")
                        .param("Authority", AUTHORITY)
                        .param("Status", "NOK"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("result=cancelled")));

        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CANCELLED);
        assertThat(gatewayClient.verifyCalls.get()).isZero();
        Integer provisionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM xui_client_provisions", Integer.class);
        assertThat(provisionCount).isZero();
    }

    private Payment payment(long telegramUserId) {
        User user = userRepository.save(User.create(telegramUserId, null, "Pay", null, UserLanguage.EN, NOW));
        Order order = orderRepository.save(Order.create(user.getId(), 100_000L, "IRT"));
        return paymentRepository.save(Payment.create(
                order.getId(),
                user.getId(),
                PaymentMethod.ZARINPAL,
                100_000L,
                100_000L,
                "IRT",
                NOW.plusSeconds(1800)
        ));
    }

    @TestConfiguration
    static class FakeGatewayConfiguration {

        @Bean
        @Primary
        ZarinpalGatewayClient fakeZarinpalGatewayClient() {
            return new FakeZarinpalGatewayClient();
        }
    }

    static final class FakeZarinpalGatewayClient implements ZarinpalGatewayClient {

        private final AtomicInteger createCalls = new AtomicInteger();
        private final AtomicInteger verifyCalls = new AtomicInteger();

        @Override
        public ZarinpalCreateResponse createPayment(ZarinpalCreateRequest request) {
            createCalls.incrementAndGet();
            assertThat(request.amount()).isEqualTo(100_000L);
            assertThat(request.currency()).isEqualTo("IRT");
            return new ZarinpalCreateResponse(true, AUTHORITY, 100, "Success", "https://pay.test/" + AUTHORITY);
        }

        @Override
        public ZarinpalVerifyResponse verifyPayment(ZarinpalVerifyRequest request) {
            verifyCalls.incrementAndGet();
            assertThat(request.amount()).isEqualTo(100_000L);
            assertThat(request.authority()).isEqualTo(AUTHORITY);
            return new ZarinpalVerifyResponse(
                    true,
                    false,
                    100,
                    "Verified",
                    "987654321",
                    "FAKE_HASH",
                    "502229******5995",
                    0,
                    "Merchant"
            );
        }

        void reset() {
            createCalls.set(0);
            verifyCalls.set(0);
        }
    }
}
