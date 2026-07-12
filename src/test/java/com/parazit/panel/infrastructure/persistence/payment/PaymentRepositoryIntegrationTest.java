package com.parazit.panel.infrastructure.persistence.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.config.persistence.JpaAuditingConfiguration;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentOperation;
import com.parazit.panel.domain.payment.PaymentOperationType;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.repository.PaymentOperationRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.infrastructure.persistence.order.OrderRepositoryAdapter;
import com.parazit.panel.infrastructure.persistence.order.SpringDataOrderRepository;
import com.parazit.panel.infrastructure.persistence.user.SpringDataUserRepository;
import com.parazit.panel.infrastructure.persistence.user.UserRepositoryAdapter;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Clock;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackageClasses = {Payment.class, Order.class, User.class})
@EnableJpaRepositories(basePackageClasses = {
        SpringDataPaymentRepository.class,
        SpringDataPaymentOperationRepository.class,
        SpringDataOrderRepository.class,
        SpringDataUserRepository.class
})
@Import({
        JpaAuditingConfiguration.class,
        PaymentRepositoryAdapter.class,
        PaymentOperationRepositoryAdapter.class,
        OrderRepositoryAdapter.class,
        UserRepositoryAdapter.class,
        MutableClockTestConfiguration.class
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class PaymentRepositoryIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");

    private final PaymentRepository paymentRepository;
    private final PaymentOperationRepository operationRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;
    private final Flyway flyway;

    PaymentRepositoryIntegrationTest(
            PaymentRepository paymentRepository,
            PaymentOperationRepository operationRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            EntityManager entityManager,
            JdbcTemplate jdbcTemplate,
            Flyway flyway,
            Clock clock
    ) {
        this.paymentRepository = paymentRepository;
        this.operationRepository = operationRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
        this.flyway = flyway;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPaymentTables(jdbcTemplate);
        entityManager.clear();
    }

    @Test
    void savesPaymentAndOperationHistoryAndQueriesByOrderUserAndWaitingStatus() {
        User user = userRepository.save(User.create(91_001L, "pay_user", "Pay", null, UserLanguage.EN, NOW));
        Order order = orderRepository.save(Order.create(user.getId(), 100_000L, "IRT"));
        Payment payment = paymentRepository.save(payment(order));
        operationRepository.save(PaymentOperation.record(payment.getId(), PaymentOperationType.CREATED, NOW, "created"));
        payment.markWaitingForPayment();
        paymentRepository.save(payment);
        entityManager.flush();
        entityManager.clear();

        assertThat(paymentRepository.findByOrderId(order.getId())).isPresent();
        assertThat(paymentRepository.findAllByOrderId(order.getId())).hasSize(1);
        assertThat(paymentRepository.findAllByUserId(user.getId())).hasSize(1);
        assertThat(paymentRepository.findWaitingPayments())
                .extracting(Payment::getStatus)
                .containsExactly(PaymentStatus.WAITING_FOR_PAYMENT);
        assertThat(operationRepository.findAllByPaymentIdOrderByOccurredAtAsc(payment.getId()))
                .extracting(PaymentOperation::getOperationType)
                .containsExactly(PaymentOperationType.CREATED);
    }

    @Test
    void enforcesOnlyOneApprovedPaymentPerOrder() {
        User user = userRepository.save(User.create(91_002L, null, "Pay", null, UserLanguage.EN, NOW));
        Order order = orderRepository.save(Order.create(user.getId(), 100_000L, "IRT"));
        Payment first = payment(order);
        first.markWaitingForPayment();
        first.markApproved(NOW, "tx-1", "auth-1");
        paymentRepository.save(first);
        entityManager.flush();
        entityManager.clear();

        Payment second = payment(order);
        second.markWaitingForPayment();
        second.markApproved(NOW, "tx-2", "auth-2");

        assertThatThrownBy(() -> {
            paymentRepository.save(second);
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    void flywayMigratesPaymentFoundation() {
        assertThat(flyway.info().applied())
                .anySatisfy(info -> {
                    assertThat(info.getVersion().getVersion()).isEqualTo("10");
                    assertThat(info.getState()).isEqualTo(MigrationState.SUCCESS);
                });
    }

    private Payment payment(Order order) {
        return Payment.create(
                order.getId(),
                order.getUserId(),
                PaymentMethod.CARD_TO_CARD,
                order.getAmount(),
                order.getAmount(),
                order.getCurrency(),
                NOW.plusSeconds(1800)
        );
    }
}
