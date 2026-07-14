package com.parazit.panel.application.wallet.payment;

import com.parazit.panel.application.port.in.wallet.GetOrCreateWalletUseCase;
import com.parazit.panel.application.port.in.wallet.payment.GetWalletOrderPaymentPreviewUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.wallet.payment.command.GetWalletOrderPaymentPreviewCommand;
import com.parazit.panel.application.wallet.payment.result.WalletOrderPaymentOutcome;
import com.parazit.panel.application.wallet.payment.result.WalletOrderPaymentPreviewResult;
import com.parazit.panel.config.properties.WalletPaymentProperties;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.wallet.Wallet;
import com.parazit.panel.domain.wallet.repository.WalletRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetWalletOrderPaymentPreviewService implements GetWalletOrderPaymentPreviewUseCase {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final GetOrCreateWalletUseCase getOrCreateWalletUseCase;
    private final WalletOrderPaymentEligibilityPolicy eligibilityPolicy;
    private final WalletPaymentProperties properties;
    private final SystemClockPort clock;

    public GetWalletOrderPaymentPreviewService(
            UserRepository userRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            WalletRepository walletRepository,
            GetOrCreateWalletUseCase getOrCreateWalletUseCase,
            WalletOrderPaymentEligibilityPolicy eligibilityPolicy,
            WalletPaymentProperties properties,
            SystemClockPort clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository must not be null");
        this.getOrCreateWalletUseCase = Objects.requireNonNull(getOrCreateWalletUseCase, "getOrCreateWalletUseCase must not be null");
        this.eligibilityPolicy = Objects.requireNonNull(eligibilityPolicy, "eligibilityPolicy must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public WalletOrderPaymentPreviewResult preview(GetWalletOrderPaymentPreviewCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        User user = userRepository.findByTelegramUserId(command.telegramUserId())
                .orElseThrow(() -> new WalletOrderPaymentException("customer account not found"));
        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new WalletOrderPaymentException("order not found"));
        getOrCreateWalletUseCase.getOrCreate(user.getId());
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new WalletOrderPaymentException("wallet not found"));
        List<Payment> payments = paymentRepository.findAllByOrderId(order.getId());
        WalletOrderPaymentEligibility eligibility = eligibilityPolicy.evaluate(user.getId(), order, payments, clock.now());
        Money amount = new Money(order.getFinalAmount(), properties.currency());
        Money balance = wallet.balance();
        Money projected = balance.amount() >= amount.amount()
                ? new Money(balance.amount() - amount.amount(), balance.currency())
                : balance;
        boolean sufficient = balance.currency() == amount.currency() && balance.amount() >= amount.amount();
        return new WalletOrderPaymentPreviewResult(
                order.getId(),
                order.getType(),
                amount,
                balance,
                projected,
                eligibility.eligible(),
                sufficient,
                eligibility.eligible() ? null : outcome(eligibility.reason())
        );
    }

    private static WalletOrderPaymentOutcome outcome(WalletOrderPaymentEligibilityReason reason) {
        return switch (reason) {
            case APPROVED_PAYMENT_EXISTS -> WalletOrderPaymentOutcome.ALREADY_PAID;
            case CONFLICTING_PAYMENT_EXISTS -> WalletOrderPaymentOutcome.CONFLICTING_PAYMENT_EXISTS;
            case FEATURE_DISABLED -> WalletOrderPaymentOutcome.WALLET_UNAVAILABLE;
            default -> WalletOrderPaymentOutcome.ORDER_NOT_ELIGIBLE;
        };
    }
}
