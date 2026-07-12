package com.parazit.panel.api.internal.payment;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test",
        "app.payment.default-expiration=PT30M"
})
@AutoConfigureMockMvc
@Import(MutableClockTestConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class PaymentControllerTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final JdbcTemplate jdbcTemplate;

    PaymentControllerTest(
            MockMvc mockMvc,
            UserRepository userRepository,
            OrderRepository orderRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPaymentTables(jdbcTemplate);
    }

    @Test
    void postCreatesPaymentAndGetEndpointsReturnSafeDtos() throws Exception {
        User user = userRepository.save(User.create(92_001L, null, "Pay", null, UserLanguage.EN, NOW));
        Order order = orderRepository.save(Order.create(user.getId(), 500_000L, "IRT"));

        String createResponse = mockMvc.perform(post("/internal/payments")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "userId": "%s",
                                  "paymentMethod": "ZARINPAL",
                                  "amount": 500000,
                                  "currency": "IRT"
                                }
                                """.formatted(order.getId(), user.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.orderId").value(order.getId().toString()))
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.method").value("ZARINPAL"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.baseAmount").value(500_000))
                .andExpect(jsonPath("$.payableAmount").value(500_000))
                .andExpect(jsonPath("$.currency").value("IRT"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.raw").doesNotExist())
                .andExpect(content().string(not(containsString("merchant"))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String paymentId = createResponse.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(get("/internal/payments/{id}", paymentId)
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId));

        mockMvc.perform(get("/internal/orders/{id}/payments", order.getId())
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(paymentId));
    }

    @Test
    void rejectsInvalidMissingAndMismatchedRequests() throws Exception {
        User user = userRepository.save(User.create(92_002L, null, "Pay", null, UserLanguage.EN, NOW));
        Order order = orderRepository.save(Order.create(user.getId(), 500_000L, "IRT"));

        mockMvc.perform(post("/internal/payments")
                        .with(httpBasic("test", "test"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        mockMvc.perform(post("/internal/payments")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "userId": "%s",
                                  "paymentMethod": "CARD_TO_CARD",
                                  "amount": 1,
                                  "currency": "IRT"
                                }
                                """.formatted(order.getId(), user.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        mockMvc.perform(get("/internal/payments/{id}", java.util.UUID.randomUUID())
                        .with(httpBasic("test", "test")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }
}
