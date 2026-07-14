package com.parazit.panel.application.wallet.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parazit.panel.application.payment.PaymentApprovalService;
import com.parazit.panel.application.port.in.wallet.DebitWalletUseCase;
import com.parazit.panel.application.port.in.wallet.GetOrCreateWalletUseCase;
import com.parazit.panel.application.wallet.command.DebitWalletCommand;
import com.parazit.panel.application.wallet.payment.command.PayOrderWithWalletCommand;
import com.parazit.panel.application.wallet.payment.result.WalletOrderPaymentOutcome;
import com.parazit.panel.application.wallet.result.WalletCreationResult;
import com.parazit.panel.application.wallet.result.WalletOperationResult;
import com.parazit.panel.config.properties.WalletPaymentProperties;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.wallet.WalletOperationOutcome;
import com.parazit.panel.domain.wallet.WalletStatus;
import com.parazit.panel.domain.wallet.WalletTransactionDirection;
import com.parazit.panel.domain.wallet.repository.WalletTransactionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PayOrderWithWalletServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-14T00:00:00Z");

    private UserRepository userRepository;
    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private DebitWalletUseCase debitWalletUseCase;
    private PaymentApprovalService paymentApprovalService;
    private WalletOrderPaymentEligibilityPolicy eligibilityPolicy;
    private PayOrderWithWalletService service;
    private UUID userId;
    private UUID orderId;
    private User user;
    private Order order;

    @BeforeEach
    void setUp() {
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        orderRepository = org.mockito.Mockito.mock(OrderRepository.class);
        paymentRepository = org.mockito.Mockito.mock(PaymentRepository.class);
        WalletTransactionRepository transactionRepository = org.mockito.Mockito.mock(WalletTransactionRepository.class);
        GetOrCreateWalletUseCase getOrCreateWalletUseCase = org.mockito.Mockito.mock(GetOrCreateWalletUseCase.class);
        debitWalletUseCase = org.mockito.Mockito.mock(DebitWalletUseCase.class);
        paymentApprovalService = org.mockito.Mockito.mock(PaymentApprovalService.class);
        eligibilityPolicy = org.mockito.Mockito.mock(WalletOrderPaymentEligibilityPolicy.class);
        service = new PayOrderWithWalletService(
                userRepository,
                orderRepository,
                paymentRepository,
                transactionRepository,
                getOrCreateWalletUseCase,
                debitWalletUseCase,
                paymentApprovalService,
                eligibilityPolicy,
                new WalletPaymentProperties(true, true, true, CurrencyCode.IRT, 0, 0, 3, Duration.ofMinutes(15)),
                () -> NOW
        );
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        user = User.create(1234L, "user", "First", null, UserLanguage.EN, NOW);
        ReflectionTestUtils.setField(user, "id", userId);
        order = Order.create(userId, 100_000L, "IRT");
        ReflectionTestUtils.setField(order, "id", orderId);
        order.markPaymentPending();
        when(userRepository.findByTelegramUserId(1234L)).thenReturn(Optional.of(user));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findAllByOrderId(orderId)).thenReturn(List.of());
        when(eligibilityPolicy.evaluate(userId, order, List.of(), NOW)).thenReturn(WalletOrderPaymentEligibility.allowed());
        when(getOrCreateWalletUseCase.getOrCreate(userId))
                .thenReturn(new WalletCreationResult(UUID.randomUUID(), new Money(100_000L, CurrencyCode.IRT), WalletStatus.ACTIVE));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());
            return payment;
        });
    }

    @Test
    void debitsWalletCreatesPaymentAndDispatchesPaidOrder() {
        UUID transactionId = UUID.randomUUID();
        when(debitWalletUseCase.debit(any(DebitWalletCommand.class))).thenReturn(new WalletOperationResult(
                UUID.randomUUID(),
                transactionId,
                new Money(100_000L, CurrencyCode.IRT),
                new Money(0L, CurrencyCode.IRT),
                new Money(100_000L, CurrencyCode.IRT),
                WalletTransactionDirection.DEBIT,
                WalletOperationOutcome.APPLIED,
                false,
                NOW
        ));

        var result = service.pay(new PayOrderWithWalletCommand(1234L, orderId, UUID.randomUUID()));

        assertThat(result.outcome()).isEqualTo(WalletOrderPaymentOutcome.PAID);
        assertThat(result.walletTransactionId()).isEqualTo(transactionId);
        verify(paymentRepository).save(any(Payment.class));
        verify(paymentApprovalService).approve(any());
    }

    @Test
    void rejectsInsufficientBalanceWithoutPaymentApproval() {
        when(debitWalletUseCase.debit(any(DebitWalletCommand.class))).thenReturn(new WalletOperationResult(
                UUID.randomUUID(),
                null,
                new Money(50_000L, CurrencyCode.IRT),
                new Money(50_000L, CurrencyCode.IRT),
                new Money(100_000L, CurrencyCode.IRT),
                WalletTransactionDirection.DEBIT,
                WalletOperationOutcome.REJECTED_INSUFFICIENT_BALANCE,
                false,
                NOW
        ));

        var result = service.pay(new PayOrderWithWalletCommand(1234L, orderId, UUID.randomUUID()));

        assertThat(result.outcome()).isEqualTo(WalletOrderPaymentOutcome.INSUFFICIENT_BALANCE);
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentApprovalService, never()).approve(any());
    }
}
