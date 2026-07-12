package com.parazit.panel.integration.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.payment.command.CreatePaymentCommand;
import com.parazit.panel.application.payment.result.PaymentResult;
import com.parazit.panel.application.port.in.payment.CreatePaymentUseCase;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.repository.PaymentOperationRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "app.payment.default-expiration=PT30M"
})
@Import(MutableClockTestConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class PaymentFoundationIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");

    private final CreatePaymentUseCase createPaymentUseCase;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentOperationRepository operationRepository;
    private final JdbcTemplate jdbcTemplate;

    PaymentFoundationIntegrationTest(
            CreatePaymentUseCase createPaymentUseCase,
            UserRepository userRepository,
            OrderRepository orderRepository,
            PaymentOperationRepository operationRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.createPaymentUseCase = createPaymentUseCase;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.operationRepository = operationRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPaymentTables(jdbcTemplate);
    }

    @Test
    void createsLocalPaymentForExistingOrderAndRecordsHistoryWithoutProvider() {
        User user = userRepository.save(User.create(93_001L, null, "Pay", null, UserLanguage.EN, NOW));
        Order order = orderRepository.save(Order.create(user.getId(), 750_000L, "IRT"));

        PaymentResult result = createPaymentUseCase.create(new CreatePaymentCommand(
                order.getId(),
                user.getId(),
                PaymentMethod.CARD_TO_CARD,
                750_000L,
                "IRT"
        ));

        assertThat(result.id()).isNotNull();
        assertThat(result.status()).isEqualTo(PaymentStatus.CREATED);
        assertThat(result.method()).isEqualTo(PaymentMethod.CARD_TO_CARD);
        assertThat(operationRepository.findAllByPaymentIdOrderByOccurredAtAsc(result.id())).hasSize(1);
    }
}
