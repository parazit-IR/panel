package com.parazit.panel.infrastructure.persistence.payment.manual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.config.persistence.JpaAuditingConfiguration;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.manual.BankCardNumber;
import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.ManualPaymentDestination;
import com.parazit.panel.domain.payment.manual.repository.ManualCardPaymentInstructionRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(properties = "spring.profiles.active=local")
@Import({
        JpaAuditingConfiguration.class,
        MutableClockTestConfiguration.class
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ManualCardPaymentInstructionRepositoryIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");
    private static final ManualPaymentDestination DESTINATION = new ManualPaymentDestination(
            "PRIMARY_CARD",
            "Example Bank",
            "Example Holder",
            BankCardNumber.parse("6037990000000014"),
            true,
            0
    );

    private final ManualCardPaymentInstructionRepository instructionRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;
    private final Flyway flyway;

    ManualCardPaymentInstructionRepositoryIntegrationTest(
            ManualCardPaymentInstructionRepository instructionRepository,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate,
            EntityManager entityManager,
            Flyway flyway
    ) {
        this.instructionRepository = instructionRepository;
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
    void savesFindsAndEnforcesActiveUniqueness() {
        Payment firstPayment = payment(901001L);
        ManualCardPaymentInstruction first = active(firstPayment, UUID.randomUUID(), 101);
        instructionRepository.save(first);
        entityManager.clear();

        assertThat(instructionRepository.findByInstructionRequestId(first.getInstructionRequestId())).isPresent();
        assertThat(instructionRepository.findActiveByPaymentId(firstPayment.getId())).isPresent();
        assertThat(instructionRepository.findActiveByCurrencyAndPayableAmount("IRT", 100_101L)).isPresent();

        Payment secondPayment = payment(901002L);
        assertThatThrownBy(() -> instructionRepository.save(active(secondPayment, UUID.randomUUID(), 101)))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    void releasesAmountReservationAfterTerminalStatus() {
        Payment firstPayment = payment(901003L);
        ManualCardPaymentInstruction first = active(firstPayment, UUID.randomUUID(), 222);
        first.expire(NOW.plus(Duration.ofMinutes(31)));
        instructionRepository.save(first);
        entityManager.clear();

        Payment secondPayment = payment(901004L);
        assertThat(instructionRepository.save(active(secondPayment, UUID.randomUUID(), 222)).getPayableAmount())
                .isEqualTo(100_222L);
    }

    @Test
    void flywayMigratesManualInstructionFoundation() {
        assertThat(flyway.info().applied())
                .anySatisfy(info -> {
                    assertThat(info.getVersion().getVersion()).isEqualTo("12");
                    assertThat(info.getState()).isEqualTo(MigrationState.SUCCESS);
                });
    }

    private ManualCardPaymentInstruction active(Payment payment, UUID requestId, long suffix) {
        ManualCardPaymentInstruction instruction = ManualCardPaymentInstruction.create(
                payment.getId(),
                requestId,
                payment.getPayableAmount(),
                suffix,
                payment.getCurrency(),
                DESTINATION,
                NOW,
                Duration.ofMinutes(30)
        );
        instruction.activate();
        return instruction;
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
}
