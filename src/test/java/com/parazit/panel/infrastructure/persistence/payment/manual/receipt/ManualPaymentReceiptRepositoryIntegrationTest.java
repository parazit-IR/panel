package com.parazit.panel.infrastructure.persistence.payment.manual.receipt;

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
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceiptStatus;
import com.parazit.panel.domain.payment.manual.receipt.repository.ManualPaymentReceiptRepository;
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
class ManualPaymentReceiptRepositoryIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");
    private static final String SHA256 = "abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd";
    private static final ManualPaymentDestination DESTINATION = new ManualPaymentDestination(
            "PRIMARY_CARD",
            "Example Bank",
            "Example Holder",
            BankCardNumber.parse("6037990000000014"),
            true,
            0
    );

    private final ManualPaymentReceiptRepository receiptRepository;
    private final ManualCardPaymentInstructionRepository instructionRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;
    private final Flyway flyway;

    ManualPaymentReceiptRepositoryIntegrationTest(
            ManualPaymentReceiptRepository receiptRepository,
            ManualCardPaymentInstructionRepository instructionRepository,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate,
            EntityManager entityManager,
            Flyway flyway
    ) {
        this.receiptRepository = receiptRepository;
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
    void savesFindsAndQueuesReceipts() {
        Payment payment = payment(904001L);
        ManualCardPaymentInstruction instruction = instruction(payment, 1_638L);
        ManualPaymentReceipt receipt = queuedReceipt(payment, instruction, payment.getUserId(), UUID.randomUUID(), SHA256, false);

        receiptRepository.save(receipt);
        entityManager.clear();

        assertThat(receiptRepository.findByReceiptRequestId(receipt.getReceiptRequestId())).isPresent();
        assertThat(receiptRepository.findActiveByInstructionId(instruction.getId())).isPresent();
        assertThat(receiptRepository.findByStorageKey(receipt.getStorageKey())).isPresent();
        assertThat(receiptRepository.findActiveByUserIdAndFileSha256(payment.getUserId(), SHA256)).isPresent();
        assertThat(receiptRepository.findAllByPaymentIdOrderBySubmittedAtDesc(payment.getId())).hasSize(1);
        assertThat(receiptRepository.findAllQueuedForReviewOrderByReviewQueuedAtAsc(10, 0))
                .extracting(ManualPaymentReceipt::getStatus)
                .containsExactly(ManualPaymentReceiptStatus.QUEUED_FOR_REVIEW);
    }

    @Test
    void enforcesOneActiveReceiptPerInstructionAndAllowsHistoryAfterWithdrawal() {
        Payment payment = payment(904002L);
        ManualCardPaymentInstruction instruction = instruction(payment, 101L);
        ManualPaymentReceipt first = receipt(payment, instruction, payment.getUserId(), UUID.randomUUID());
        receiptRepository.save(first);

        assertThatThrownBy(() -> receiptRepository.save(receipt(payment, instruction, payment.getUserId(), UUID.randomUUID())))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);

        first.markStored("local", "manual-receipts/2026/07/a/file.png", "receipt.png", "image/png", 100, SHA256, false);
        first.queueForReview(NOW.plusSeconds(1));
        first.withdraw(NOW.plusSeconds(2));
        receiptRepository.save(first);
        entityManager.clear();

        assertThat(receiptRepository.save(receipt(payment, instruction, payment.getUserId(), UUID.randomUUID())).getStatus())
                .isEqualTo(ManualPaymentReceiptStatus.UPLOADING);
    }

    @Test
    void enforcesRequiredStoredMetadataForQueuedReceipts() {
        Payment payment = payment(904003L);
        ManualCardPaymentInstruction instruction = instruction(payment, 102L);
        ManualPaymentReceipt receipt = receipt(payment, instruction, payment.getUserId(), UUID.randomUUID());
        receipt.markStored("local", "manual-receipts/2026/07/b/file.png", "receipt.png", "image/png", 100, SHA256, false);
        receipt.queueForReview(NOW.plusSeconds(1));
        receiptRepository.save(receipt);
        entityManager.clear();

        assertThat(jdbcTemplate.queryForObject("select count(*) from manual_payment_receipts", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void flywayMigratesManualReceiptWorkflow() {
        assertThat(flyway.info().applied())
                .anySatisfy(info -> {
                    assertThat(info.getVersion().getVersion()).isEqualTo("13");
                    assertThat(info.getState()).isEqualTo(MigrationState.SUCCESS);
                });
    }

    private ManualPaymentReceipt queuedReceipt(
            Payment payment,
            ManualCardPaymentInstruction instruction,
            UUID userId,
            UUID requestId,
            String sha256,
            boolean duplicateHashDetected
    ) {
        ManualPaymentReceipt receipt = receipt(payment, instruction, userId, requestId);
        receipt.markStored("local", "manual-receipts/2026/07/" + requestId + "/file.png", "receipt.png", "image/png", 100, sha256, duplicateHashDetected);
        receipt.queueForReview(NOW.plusSeconds(1));
        return receipt;
    }

    private ManualPaymentReceipt receipt(Payment payment, ManualCardPaymentInstruction instruction, UUID userId, UUID requestId) {
        return ManualPaymentReceipt.createUploading(
                requestId,
                payment.getId(),
                instruction.getId(),
                userId,
                "receipt.png",
                instruction.getPayableAmount(),
                "TRK-1",
                "1234",
                NOW.minusSeconds(30),
                "synthetic",
                NOW
        );
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
}
