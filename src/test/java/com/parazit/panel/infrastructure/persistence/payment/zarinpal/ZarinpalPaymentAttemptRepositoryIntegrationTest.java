package com.parazit.panel.infrastructure.persistence.payment.zarinpal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.config.persistence.JpaAuditingConfiguration;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalPaymentAttempt;
import com.parazit.panel.domain.payment.zarinpal.repository.ZarinpalPaymentAttemptRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.infrastructure.persistence.order.OrderRepositoryAdapter;
import com.parazit.panel.infrastructure.persistence.order.SpringDataOrderRepository;
import com.parazit.panel.infrastructure.persistence.payment.PaymentRepositoryAdapter;
import com.parazit.panel.infrastructure.persistence.payment.SpringDataPaymentRepository;
import com.parazit.panel.infrastructure.persistence.user.SpringDataUserRepository;
import com.parazit.panel.infrastructure.persistence.user.UserRepositoryAdapter;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(properties = "spring.profiles.active=local")
@Import({
        JpaAuditingConfiguration.class,
        MutableClockTestConfiguration.class
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ZarinpalPaymentAttemptRepositoryIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");

    private final ZarinpalPaymentAttemptRepository attemptRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;
    private final Flyway flyway;

    ZarinpalPaymentAttemptRepositoryIntegrationTest(
            ZarinpalPaymentAttemptRepository attemptRepository,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate,
            EntityManager entityManager,
            Flyway flyway
    ) {
        this.attemptRepository = attemptRepository;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
        this.flyway = flyway;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPaymentTables(jdbcTemplate);
        entityManager.clear();
    }

    @Test
    void savesAndFindsAttemptByRequestAuthorityAndVerifiedPayment() {
        Payment payment = payment();
        UUID requestId = UUID.randomUUID();
        ZarinpalPaymentAttempt attempt = ZarinpalPaymentAttempt.create(payment.getId(), requestId, 100_000L);
        attempt.markRequesting(NOW);
        attempt.markRedirectReady("A000000000000000000000000000123456", NOW, "100");
        attempt.markCallbackReceived(NOW);
        attempt.markVerifying(NOW);
        attempt.markVerified("987654321", "100", "hash", "502229******5995", NOW);
        attemptRepository.save(attempt);
        entityManager.clear();

        assertThat(attemptRepository.findByRequestId(requestId)).isPresent();
        assertThat(attemptRepository.findByAuthority("A000000000000000000000000000123456")).isPresent();
        assertThat(attemptRepository.findVerifiedByPaymentId(payment.getId())).isPresent();
        assertThat(attemptRepository.findAllByPaymentIdOrderByCreatedAtDesc(payment.getId())).hasSize(1);
    }

    @Test
    void enforcesUniqueRequestIdAuthorityAndReferenceId() {
        Payment payment = payment();
        UUID requestId = UUID.randomUUID();
        ZarinpalPaymentAttempt first = ready(payment, requestId, "A000000000000000000000000000123456", "111");
        attemptRepository.save(first);
        entityManager.clear();

        assertThatThrownBy(() -> {
            attemptRepository.save(ready(payment, requestId, "A000000000000000000000000000654321", "222"));
        }).isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    void flywayMigratesZarinpalAttemptFoundation() {
        assertThat(flyway.info().applied())
                .anySatisfy(info -> {
                    assertThat(info.getVersion().getVersion()).isEqualTo("11");
                    assertThat(info.getState()).isEqualTo(MigrationState.SUCCESS);
                });
    }

    private ZarinpalPaymentAttempt ready(Payment payment, UUID requestId, String authority, String referenceId) {
        ZarinpalPaymentAttempt attempt = ZarinpalPaymentAttempt.create(payment.getId(), requestId, 100_000L);
        attempt.markRequesting(NOW);
        attempt.markRedirectReady(authority, NOW, "100");
        attempt.markCallbackReceived(NOW);
        attempt.markVerifying(NOW);
        attempt.markVerified(referenceId, "100", null, null, NOW);
        return attempt;
    }

    private Payment payment() {
        User user = userRepository.save(User.create(Math.abs(UUID.randomUUID().getMostSignificantBits()), null, "Pay", null, UserLanguage.EN, NOW));
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
}
