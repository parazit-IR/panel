package com.parazit.panel.integration.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.payment.ApprovePaymentCommand;
import com.parazit.panel.application.payment.PaymentApprovalException;
import com.parazit.panel.application.payment.PaymentApprovalResult;
import com.parazit.panel.application.payment.PaymentApprovalService;
import com.parazit.panel.application.payment.PaymentApprovalSource;
import com.parazit.panel.application.payment.manual.review.ManualPaymentReviewConflictException;
import com.parazit.panel.application.payment.manual.review.ManualPaymentReviewNotAllowedException;
import com.parazit.panel.application.payment.manual.review.command.ApproveManualPaymentReviewCommand;
import com.parazit.panel.application.payment.manual.review.command.ClaimManualPaymentReviewCommand;
import com.parazit.panel.application.payment.manual.review.command.RejectManualPaymentReviewCommand;
import com.parazit.panel.application.payment.manual.review.result.ManualPaymentReviewResult;
import com.parazit.panel.application.port.in.payment.manual.review.ApproveManualPaymentReviewUseCase;
import com.parazit.panel.application.port.in.payment.manual.review.ClaimManualPaymentReviewUseCase;
import com.parazit.panel.application.port.in.payment.manual.review.RejectManualPaymentReviewUseCase;
import com.parazit.panel.application.port.out.security.CurrentOperatorProvider;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.OrderStatus;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.manual.BankCardNumber;
import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.ManualPaymentDestination;
import com.parazit.panel.domain.payment.manual.ManualPaymentInstructionStatus;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceiptStatus;
import com.parazit.panel.domain.payment.manual.receipt.repository.ManualPaymentReceiptRepository;
import com.parazit.panel.domain.payment.manual.repository.ManualCardPaymentInstructionRepository;
import com.parazit.panel.domain.payment.manual.review.ManualPaymentRejectionReason;
import com.parazit.panel.domain.payment.manual.review.ManualPaymentReview;
import com.parazit.panel.domain.payment.manual.review.ManualPaymentReviewStatus;
import com.parazit.panel.domain.payment.manual.review.repository.ManualPaymentReviewRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "app.payment.manual-review.require-tracking-number=false",
        "app.payment.manual-review.require-sender-card-last-four=false",
        "app.payment.manual-review.require-operator-note-on-approval=false"
})
@Import({
        MutableClockTestConfiguration.class,
        ConcurrentPaymentApprovalIntegrationTest.ThreadLocalOperatorConfiguration.class
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ConcurrentPaymentApprovalIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");
    private static final String OPERATOR = "operator-1";
    private static final ManualPaymentDestination DESTINATION = new ManualPaymentDestination(
            "PRIMARY_CARD",
            "Example Bank",
            "Example Holder",
            BankCardNumber.parse("6037990000000014"),
            true,
            0
    );

    private final PaymentApprovalService paymentApprovalService;
    private final ClaimManualPaymentReviewUseCase claimReviewUseCase;
    private final ApproveManualPaymentReviewUseCase approveReviewUseCase;
    private final RejectManualPaymentReviewUseCase rejectReviewUseCase;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ManualCardPaymentInstructionRepository instructionRepository;
    private final ManualPaymentReceiptRepository receiptRepository;
    private final ManualPaymentReviewRepository reviewRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ThreadLocalCurrentOperatorProvider operatorProvider;

    ConcurrentPaymentApprovalIntegrationTest(
            PaymentApprovalService paymentApprovalService,
            ClaimManualPaymentReviewUseCase claimReviewUseCase,
            ApproveManualPaymentReviewUseCase approveReviewUseCase,
            RejectManualPaymentReviewUseCase rejectReviewUseCase,
            UserRepository userRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            ManualCardPaymentInstructionRepository instructionRepository,
            ManualPaymentReceiptRepository receiptRepository,
            ManualPaymentReviewRepository reviewRepository,
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            CurrentOperatorProvider operatorProvider
    ) {
        this.paymentApprovalService = paymentApprovalService;
        this.claimReviewUseCase = claimReviewUseCase;
        this.approveReviewUseCase = approveReviewUseCase;
        this.rejectReviewUseCase = rejectReviewUseCase;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.instructionRepository = instructionRepository;
        this.receiptRepository = receiptRepository;
        this.reviewRepository = reviewRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.operatorProvider = (ThreadLocalCurrentOperatorProvider) operatorProvider;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPaymentTables(jdbcTemplate);
        operatorProvider.clear();
    }

    @Test
    void zarinpalAndManualApprovalForSameOrderAllowOnlyOneApprovedPayment() throws Exception {
        User user = userRepository.save(User.create(910001L, null, "Pay", null, UserLanguage.EN, NOW));
        Order order = orderRepository.save(Order.create(user.getId(), 100_000L, "IRT"));
        Payment zarinpalPayment = waitingPayment(order, user, PaymentMethod.ZARINPAL, 100_000L);
        Payment manualPayment = waitingReviewPayment(order, user, 101_638L);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstApprovedInsideOpenTransaction = new CountDownLatch(1);
        CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);

        try {
            Future<PaymentApprovalResult> first = executor.submit(() -> transactionTemplate.execute(status -> {
                PaymentApprovalResult result = paymentApprovalService.approve(new ApprovePaymentCommand(
                        zarinpalPayment.getId(),
                        PaymentApprovalSource.ZARINPAL_VERIFICATION,
                        "987654321",
                        "A000000000000000000000000000123456",
                        NOW
                ));
                firstApprovedInsideOpenTransaction.countDown();
                awaitUnchecked(releaseFirstTransaction);
                return result;
            }));
            assertThat(firstApprovedInsideOpenTransaction.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                return paymentApprovalService.approve(new ApprovePaymentCommand(
                        manualPayment.getId(),
                        PaymentApprovalSource.MANUAL_OPERATOR_REVIEW,
                        "manual-review",
                        null,
                        NOW.plusSeconds(1)
                ));
            });
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();

            releaseFirstTransaction.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS).newlyApproved()).isTrue();
            assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(PaymentApprovalException.class);

            assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(paymentRepository.findAllByOrderId(order.getId()))
                    .filteredOn(payment -> payment.getStatus() == PaymentStatus.APPROVED)
                    .singleElement()
                    .extracting(Payment::getId)
                    .isEqualTo(zarinpalPayment.getId());
            assertThat(paymentRepository.findById(manualPayment.getId()).orElseThrow().getStatus())
                    .isEqualTo(PaymentStatus.WAITING_FOR_REVIEW);
        } finally {
            releaseFirstTransaction.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void twoOperatorsClaimingSameManualReviewLeaveOneClaimOwner() throws Exception {
        ManualReceiptFixture fixture = manualReceiptFixture(910002L);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstClaimedInsideOpenTransaction = new CountDownLatch(1);
        CountDownLatch releaseFirstClaimTransaction = new CountDownLatch(1);
        CountDownLatch secondClaimStarted = new CountDownLatch(1);

        try {
            Future<ManualPaymentReviewResult> first = executor.submit(() -> operatorProvider.callAs("operator-a",
                    () -> transactionTemplate.execute(status -> {
                        ManualPaymentReviewResult result = claimReviewUseCase.claim(
                                new ClaimManualPaymentReviewCommand(fixture.receiptId())
                        );
                        firstClaimedInsideOpenTransaction.countDown();
                        awaitUnchecked(releaseFirstClaimTransaction);
                        return result;
                    })
            ));
            assertThat(firstClaimedInsideOpenTransaction.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> second = executor.submit(() -> operatorProvider.callAs("operator-b", () -> {
                secondClaimStarted.countDown();
                return claimReviewUseCase.claim(new ClaimManualPaymentReviewCommand(fixture.receiptId()));
            }));
            assertThat(secondClaimStarted.await(5, TimeUnit.SECONDS)).isTrue();

            releaseFirstClaimTransaction.countDown();
            ManualPaymentReviewResult firstResult = first.get(5, TimeUnit.SECONDS);
            assertThat(firstResult.status()).isEqualTo(ManualPaymentReviewStatus.CLAIMED);
            assertThat(firstResult.reviewerId()).isEqualTo("operator-a");
            assertThat(firstResult.changed()).isTrue();
            assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .cause()
                    .isInstanceOf(ManualPaymentReviewConflictException.class);

            ManualPaymentReview review = reviewRepository.findByReceiptId(fixture.receiptId()).orElseThrow();
            assertThat(review.getStatus()).isEqualTo(ManualPaymentReviewStatus.CLAIMED);
            assertThat(review.getReviewerId()).isEqualTo("operator-a");
        } finally {
            releaseFirstClaimTransaction.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            operatorProvider.clear();
        }
    }

    @Test
    void concurrentApproveAndRejectForSameManualReviewLeaveOneTerminalDecision() throws Exception {
        ManualReviewFixture fixture = manualReviewFixture(910003L);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch approvedInsideOpenTransaction = new CountDownLatch(1);
        CountDownLatch releaseApprovalTransaction = new CountDownLatch(1);
        CountDownLatch rejectStarted = new CountDownLatch(1);

        try {
            Future<ManualPaymentReviewResult> approval = executor.submit(() -> operatorProvider.callAs(OPERATOR,
                    () -> transactionTemplate.execute(status -> {
                        ManualPaymentReviewResult result = approveReviewUseCase.approve(
                                new ApproveManualPaymentReviewCommand(fixture.receiptId(), "approved")
                        );
                        approvedInsideOpenTransaction.countDown();
                        awaitUnchecked(releaseApprovalTransaction);
                        return result;
                    })
            ));
            assertThat(approvedInsideOpenTransaction.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> rejection = executor.submit(() -> operatorProvider.callAs(OPERATOR, () -> {
                rejectStarted.countDown();
                return rejectReviewUseCase.reject(new RejectManualPaymentReviewCommand(
                        fixture.receiptId(),
                        ManualPaymentRejectionReason.OTHER,
                        "rejected"
                ));
            }));
            assertThat(rejectStarted.await(5, TimeUnit.SECONDS)).isTrue();

            releaseApprovalTransaction.countDown();
            assertThat(approval.get(5, TimeUnit.SECONDS).status()).isEqualTo(ManualPaymentReviewStatus.APPROVED);
            assertThatThrownBy(() -> rejection.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .cause()
                    .isInstanceOfAny(
                            ManualPaymentReviewConflictException.class,
                            ManualPaymentReviewNotAllowedException.class
                    );

            assertThat(reviewRepository.findByReceiptId(fixture.receiptId()).orElseThrow().getStatus())
                    .isEqualTo(ManualPaymentReviewStatus.APPROVED);
            assertThat(receiptRepository.findById(fixture.receiptId()).orElseThrow().getStatus())
                    .isEqualTo(ManualPaymentReceiptStatus.APPROVED);
            assertThat(instructionRepository.findById(fixture.instructionId()).orElseThrow().getStatus())
                    .isEqualTo(ManualPaymentInstructionStatus.COMPLETED);
            assertThat(paymentRepository.findById(fixture.paymentId()).orElseThrow().getStatus())
                    .isEqualTo(PaymentStatus.APPROVED);
        } finally {
            releaseApprovalTransaction.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            operatorProvider.clear();
        }
    }

    private Payment waitingPayment(Order order, User user, PaymentMethod method, long payableAmount) {
        Payment payment = Payment.create(
                order.getId(),
                user.getId(),
                method,
                100_000L,
                payableAmount,
                "IRT",
                NOW.plusSeconds(1800)
        );
        payment.markWaitingForPayment();
        return paymentRepository.save(payment);
    }

    private Payment waitingReviewPayment(Order order, User user, long payableAmount) {
        Payment payment = waitingPayment(order, user, PaymentMethod.CARD_TO_CARD, payableAmount);
        payment.markReceiptSubmitted(NOW);
        payment.markWaitingForReview();
        return paymentRepository.save(payment);
    }

    private ManualReviewFixture manualReviewFixture(long telegramUserId) {
        ManualReceiptFixture fixture = manualReceiptFixture(telegramUserId);
        ManualPaymentReceipt receipt = receiptRepository.findById(fixture.receiptId()).orElseThrow();
        ManualCardPaymentInstruction instruction = instructionRepository.findById(fixture.instructionId()).orElseThrow();
        Order order = orderRepository.findById(paymentRepository.findById(fixture.paymentId()).orElseThrow().getOrderId())
                .orElseThrow();

        ManualPaymentReview review = ManualPaymentReview.create(
                receipt.getId(),
                fixture.paymentId(),
                order.getId(),
                instruction.getPayableAmount(),
                receipt.getClaimedAmount(),
                false
        );
        review.claim(OPERATOR, NOW);
        reviewRepository.save(review);
        return new ManualReviewFixture(fixture.paymentId(), fixture.instructionId(), fixture.receiptId());
    }

    private ManualReceiptFixture manualReceiptFixture(long telegramUserId) {
        User user = userRepository.save(User.create(telegramUserId, null, "Pay", null, UserLanguage.EN, NOW));
        Order order = orderRepository.save(Order.create(user.getId(), 100_000L, "IRT"));
        Payment payment = waitingReviewPayment(order, user, 101_638L);
        ManualCardPaymentInstruction instruction = ManualCardPaymentInstruction.create(
                payment.getId(),
                UUID.randomUUID(),
                100_000L,
                1_638L,
                "IRT",
                DESTINATION,
                NOW,
                Duration.ofMinutes(30)
        );
        instruction.activate();
        instruction.markReceiptPending(NOW);
        instruction = instructionRepository.save(instruction);

        ManualPaymentReceipt receipt = ManualPaymentReceipt.createUploading(
                UUID.randomUUID(),
                payment.getId(),
                instruction.getId(),
                user.getId(),
                "receipt.png",
                instruction.getPayableAmount(),
                "TRK-1",
                "1234",
                NOW.minusSeconds(60),
                "synthetic",
                NOW
        );
        receipt.markStored(
                "test",
                "manual-receipts/test/receipt.png",
                "receipt.png",
                "image/png",
                12L,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                false
        );
        receipt.queueForReview(NOW);
        receipt = receiptRepository.save(receipt);

        return new ManualReceiptFixture(payment.getId(), instruction.getId(), receipt.getId());
    }

    private static void awaitUnchecked(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for test latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for test latch", exception);
        }
    }

    private record ManualReviewFixture(UUID paymentId, UUID instructionId, UUID receiptId) {
    }

    private record ManualReceiptFixture(UUID paymentId, UUID instructionId, UUID receiptId) {
    }

    @TestConfiguration
    static class ThreadLocalOperatorConfiguration {

        @Bean
        @Primary
        CurrentOperatorProvider threadLocalCurrentOperatorProvider() {
            return new ThreadLocalCurrentOperatorProvider();
        }
    }

    static final class ThreadLocalCurrentOperatorProvider implements CurrentOperatorProvider {

        private final ThreadLocal<String> operatorId = new ThreadLocal<>();

        @Override
        public String currentOperatorId() {
            String current = operatorId.get();
            if (current == null || current.isBlank()) {
                throw new IllegalStateException("Test operator identity is required");
            }
            return current;
        }

        <T> T callAs(String operator, Callable<T> callable) throws Exception {
            operatorId.set(operator);
            try {
                return callable.call();
            } finally {
                operatorId.remove();
            }
        }

        void clear() {
            operatorId.remove();
        }
    }
}
