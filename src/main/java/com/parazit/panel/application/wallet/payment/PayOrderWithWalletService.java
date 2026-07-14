package com.parazit.panel.application.wallet.payment;

import com.parazit.panel.application.payment.ApprovePaymentCommand;
import com.parazit.panel.application.payment.PaymentApprovalService;
import com.parazit.panel.application.payment.PaymentApprovalSource;
import com.parazit.panel.application.port.in.wallet.DebitWalletUseCase;
import com.parazit.panel.application.port.in.wallet.GetOrCreateWalletUseCase;
import com.parazit.panel.application.port.in.wallet.payment.PayOrderWithWalletUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.wallet.command.DebitWalletCommand;
import com.parazit.panel.application.wallet.payment.command.PayOrderWithWalletCommand;
import com.parazit.panel.application.wallet.payment.result.WalletOrderPaymentOutcome;
import com.parazit.panel.application.wallet.payment.result.WalletOrderPaymentResult;
import com.parazit.panel.application.wallet.result.WalletOperationResult;
import com.parazit.panel.config.properties.WalletPaymentProperties;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.wallet.WalletOperationOutcome;
import com.parazit.panel.domain.wallet.WalletTransaction;
import com.parazit.panel.domain.wallet.WalletTransactionType;
import com.parazit.panel.domain.wallet.repository.WalletTransactionRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayOrderWithWalletService implements PayOrderWithWalletUseCase {

    public static final String REFERENCE_TYPE = "ORDER";
    public static final String DESCRIPTION_CODE = "wallet.purchase";

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final GetOrCreateWalletUseCase getOrCreateWalletUseCase;
    private final DebitWalletUseCase debitWalletUseCase;
    private final PaymentApprovalService paymentApprovalService;
    private final WalletOrderPaymentEligibilityPolicy eligibilityPolicy;
    private final WalletPaymentProperties properties;
    private final SystemClockPort clock;

    public PayOrderWithWalletService(
            UserRepository userRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            WalletTransactionRepository walletTransactionRepository,
            GetOrCreateWalletUseCase getOrCreateWalletUseCase,
            DebitWalletUseCase debitWalletUseCase,
            PaymentApprovalService paymentApprovalService,
            WalletOrderPaymentEligibilityPolicy eligibilityPolicy,
            WalletPaymentProperties properties,
            SystemClockPort clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.walletTransactionRepository = Objects.requireNonNull(walletTransactionRepository, "walletTransactionRepository must not be null");
        this.getOrCreateWalletUseCase = Objects.requireNonNull(getOrCreateWalletUseCase, "getOrCreateWalletUseCase must not be null");
        this.debitWalletUseCase = Objects.requireNonNull(debitWalletUseCase, "debitWalletUseCase must not be null");
        this.paymentApprovalService = Objects.requireNonNull(paymentApprovalService, "paymentApprovalService must not be null");
        this.eligibilityPolicy = Objects.requireNonNull(eligibilityPolicy, "eligibilityPolicy must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public WalletOrderPaymentResult pay(PayOrderWithWalletCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        User user = userRepository.findByTelegramUserId(command.telegramUserId())
                .orElseThrow(() -> new WalletOrderPaymentException("customer account not found"));
        Order order = orderRepository.findByIdForUpdate(command.orderId())
                .orElseThrow(() -> new WalletOrderPaymentException("order not found"));
        List<Payment> payments = paymentRepository.findAllByOrderId(order.getId());
        Payment approved = approvedPayment(payments);
        if (approved != null) {
            return alreadyPaid(order, approved);
        }

        Instant now = clock.now();
        WalletOrderPaymentEligibility eligibility = eligibilityPolicy.evaluate(user.getId(), order, payments, now);
        if (!eligibility.eligible()) {
            return rejected(order, eligibility.reason());
        }

        getOrCreateWalletUseCase.getOrCreate(user.getId());
        Money amount = new Money(order.getFinalAmount(), properties.currency());
        WalletOperationResult debit = debitWalletUseCase.debit(new DebitWalletCommand(
                user.getId(),
                amount,
                WalletTransactionType.PURCHASE,
                REFERENCE_TYPE,
                order.getId(),
                idempotencyKey(order),
                DESCRIPTION_CODE
        ));
        if (debit.outcome() != WalletOperationOutcome.APPLIED && debit.outcome() != WalletOperationOutcome.REPLAYED) {
            return rejectedDebit(order, debit);
        }

        Payment payment = createWalletPayment(order, user, debit, now);
        paymentRepository.save(payment);
        paymentApprovalService.approve(new ApprovePaymentCommand(
                payment.getId(),
                PaymentApprovalSource.WALLET_PAYMENT,
                null,
                null,
                debit.occurredAt()
        ));
        return new WalletOrderPaymentResult(
                order.getId(),
                payment.getId(),
                debit.transactionId(),
                amount,
                debit.balanceBefore(),
                debit.balanceAfter(),
                debit.replayed() ? WalletOrderPaymentOutcome.ALREADY_PAID : WalletOrderPaymentOutcome.PAID,
                debit.replayed(),
                debit.occurredAt()
        );
    }

    private Payment createWalletPayment(Order order, User user, WalletOperationResult debit, Instant now) {
        Payment payment = Payment.create(
                order.getId(),
                user.getId(),
                PaymentMethod.WALLET,
                order.getFinalAmount(),
                order.getFinalAmount(),
                order.getCurrency(),
                now.plus(properties.paymentTtl())
        );
        payment.markWaitingForPayment();
        payment.attachWalletTransaction(debit.transactionId());
        return payment;
    }

    private WalletOrderPaymentResult alreadyPaid(Order order, Payment payment) {
        WalletTransaction transaction = payment.getWalletTransactionId() == null
                ? null
                : walletTransactionRepository.findById(payment.getWalletTransactionId()).orElse(null);
        Money amount = new Money(order.getFinalAmount(), properties.currency());
        return new WalletOrderPaymentResult(
                order.getId(),
                payment.getId(),
                payment.getWalletTransactionId(),
                amount,
                transaction == null ? null : transaction.balanceBefore(),
                transaction == null ? null : transaction.balanceAfter(),
                WalletOrderPaymentOutcome.ALREADY_PAID,
                true,
                payment.getApprovedAt()
        );
    }

    private WalletOrderPaymentResult rejected(Order order, WalletOrderPaymentEligibilityReason reason) {
        WalletOrderPaymentOutcome outcome = switch (reason) {
            case APPROVED_PAYMENT_EXISTS -> WalletOrderPaymentOutcome.ALREADY_PAID;
            case CONFLICTING_PAYMENT_EXISTS -> WalletOrderPaymentOutcome.CONFLICTING_PAYMENT_EXISTS;
            case FEATURE_DISABLED -> WalletOrderPaymentOutcome.WALLET_UNAVAILABLE;
            default -> WalletOrderPaymentOutcome.ORDER_NOT_ELIGIBLE;
        };
        return new WalletOrderPaymentResult(order.getId(), null, null, new Money(order.getFinalAmount(), properties.currency()),
                null, null, outcome, false, null);
    }

    private WalletOrderPaymentResult rejectedDebit(Order order, WalletOperationResult debit) {
        WalletOrderPaymentOutcome outcome = debit.outcome() == WalletOperationOutcome.REJECTED_INSUFFICIENT_BALANCE
                ? WalletOrderPaymentOutcome.INSUFFICIENT_BALANCE
                : WalletOrderPaymentOutcome.WALLET_UNAVAILABLE;
        return new WalletOrderPaymentResult(
                order.getId(),
                null,
                null,
                debit.amount(),
                debit.balanceBefore(),
                debit.balanceAfter(),
                outcome,
                false,
                null
        );
    }

    private static Payment approvedPayment(List<Payment> payments) {
        return payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.APPROVED)
                .max(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private static String idempotencyKey(Order order) {
        return "wallet-purchase:" + order.getId();
    }
}
