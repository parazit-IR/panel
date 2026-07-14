package com.parazit.panel.application.wallet.topup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parazit.panel.application.port.in.wallet.CreditWalletUseCase;
import com.parazit.panel.application.wallet.command.CreditWalletCommand;
import com.parazit.panel.application.wallet.result.WalletOperationResult;
import com.parazit.panel.application.wallet.topup.command.HandleApprovedWalletTopUpCommand;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.wallet.WalletOperationOutcome;
import com.parazit.panel.domain.wallet.WalletTransactionDirection;
import com.parazit.panel.domain.wallet.WalletTransactionType;
import com.parazit.panel.domain.wallet.topup.WalletTopUpRequest;
import com.parazit.panel.domain.wallet.topup.WalletTopUpStatus;
import com.parazit.panel.domain.wallet.topup.repository.WalletTopUpRequestRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class HandleApprovedWalletTopUpServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-14T00:00:00Z");

    private PaymentRepository paymentRepository;
    private WalletTopUpRequestRepository requestRepository;
    private CreditWalletUseCase creditWalletUseCase;
    private HandleApprovedWalletTopUpService service;
    private UUID userId;
    private UUID requestId;
    private UUID paymentId;
    private WalletTopUpRequest request;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentRepository = org.mockito.Mockito.mock(PaymentRepository.class);
        requestRepository = org.mockito.Mockito.mock(WalletTopUpRequestRepository.class);
        creditWalletUseCase = org.mockito.Mockito.mock(CreditWalletUseCase.class);
        service = new HandleApprovedWalletTopUpService(paymentRepository, requestRepository, creditWalletUseCase, () -> NOW);

        userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        requestId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        request = WalletTopUpRequest.create(
                userId,
                walletId,
                new Money(250_000, CurrencyCode.IRT),
                "request-key",
                NOW.minusSeconds(10),
                Duration.ofMinutes(30)
        );
        ReflectionTestUtils.setField(request, "id", requestId);
        payment = Payment.createWalletTopUp(
                userId,
                requestId,
                PaymentMethod.CARD_TO_CARD,
                250_000,
                250_000,
                "IRT",
                NOW.plus(Duration.ofMinutes(30))
        );
        ReflectionTestUtils.setField(payment, "id", paymentId);
        request.attachPayment(paymentId, NOW);
        payment.markWaitingForPayment();
        payment.markApproved(NOW, "review-1", null);

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(WalletTopUpRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void creditsWalletWithTopUpLedgerEntry() {
        UUID transactionId = UUID.randomUUID();
        when(creditWalletUseCase.credit(any(CreditWalletCommand.class))).thenReturn(new WalletOperationResult(
                request.getWalletId(),
                transactionId,
                new Money(0, CurrencyCode.IRT),
                new Money(250_000, CurrencyCode.IRT),
                new Money(250_000, CurrencyCode.IRT),
                WalletTransactionDirection.CREDIT,
                WalletOperationOutcome.APPLIED,
                false,
                NOW
        ));

        var result = service.handle(new HandleApprovedWalletTopUpCommand(paymentId, requestId, UUID.randomUUID()));

        assertThat(result.outcome()).isEqualTo(WalletTopUpApprovalOutcome.CREDITED);
        assertThat(request.getStatus()).isEqualTo(WalletTopUpStatus.CREDITED);
        ArgumentCaptor<CreditWalletCommand> command = ArgumentCaptor.forClass(CreditWalletCommand.class);
        verify(creditWalletUseCase).credit(command.capture());
        assertThat(command.getValue().type()).isEqualTo(WalletTransactionType.TOP_UP);
        assertThat(command.getValue().referenceType()).isEqualTo("WALLET_TOP_UP");
        assertThat(command.getValue().referenceId()).isEqualTo(requestId);
        assertThat(command.getValue().idempotencyKey()).isEqualTo("wallet-top-up:" + requestId);
    }

    @Test
    void replaysExistingLedgerCredit() {
        UUID transactionId = UUID.randomUUID();
        when(creditWalletUseCase.credit(any(CreditWalletCommand.class))).thenReturn(new WalletOperationResult(
                request.getWalletId(),
                transactionId,
                new Money(0, CurrencyCode.IRT),
                new Money(250_000, CurrencyCode.IRT),
                new Money(250_000, CurrencyCode.IRT),
                WalletTransactionDirection.CREDIT,
                WalletOperationOutcome.REPLAYED,
                true,
                NOW
        ));

        var result = service.handle(new HandleApprovedWalletTopUpCommand(paymentId, requestId, UUID.randomUUID()));

        assertThat(result.outcome()).isEqualTo(WalletTopUpApprovalOutcome.ALREADY_CREDITED);
        assertThat(result.replayed()).isTrue();
        assertThat(request.getStatus()).isEqualTo(WalletTopUpStatus.CREDITED);
    }
}
