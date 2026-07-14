package com.parazit.panel.application.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parazit.panel.application.port.in.wallet.GetOrCreateWalletUseCase;
import com.parazit.panel.application.wallet.command.CreditWalletCommand;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.wallet.Wallet;
import com.parazit.panel.domain.wallet.WalletOperationOutcome;
import com.parazit.panel.domain.wallet.WalletTransaction;
import com.parazit.panel.domain.wallet.WalletTransactionDirection;
import com.parazit.panel.domain.wallet.WalletTransactionType;
import com.parazit.panel.domain.wallet.repository.WalletRepository;
import com.parazit.panel.domain.wallet.repository.WalletTransactionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CreditWalletServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-14T00:00:00Z");

    private GetOrCreateWalletUseCase getOrCreateWalletUseCase;
    private WalletRepository walletRepository;
    private WalletTransactionRepository transactionRepository;
    private CreditWalletService service;
    private UUID userId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        getOrCreateWalletUseCase = org.mockito.Mockito.mock(GetOrCreateWalletUseCase.class);
        walletRepository = org.mockito.Mockito.mock(WalletRepository.class);
        transactionRepository = org.mockito.Mockito.mock(WalletTransactionRepository.class);
        service = new CreditWalletService(getOrCreateWalletUseCase, walletRepository, transactionRepository, () -> NOW);
        userId = UUID.randomUUID();
        wallet = Wallet.create(userId, CurrencyCode.IRT);
        ReflectionTestUtils.setField(wallet, "id", UUID.randomUUID());
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(WalletTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void appliesCreditWithOneLedgerEntry() {
        when(transactionRepository.findByWalletIdAndIdempotencyKey(wallet.getId(), "key-1")).thenReturn(Optional.empty());

        var result = service.credit(command("key-1", 100));

        assertThat(result.outcome()).isEqualTo(WalletOperationOutcome.APPLIED);
        assertThat(result.balanceBefore()).isEqualTo(new Money(0, CurrencyCode.IRT));
        assertThat(result.balanceAfter()).isEqualTo(new Money(100, CurrencyCode.IRT));
        assertThat(wallet.balance()).isEqualTo(new Money(100, CurrencyCode.IRT));
        verify(transactionRepository).save(any(WalletTransaction.class));
        verify(walletRepository).save(wallet);
    }

    @Test
    void replaysSameIdempotencyKeyAndRejectsConflictingKeyReuse() {
        WalletTransaction existing = WalletTransaction.post(
                wallet.getId(),
                userId,
                WalletTransactionType.SYSTEM_CREDIT,
                WalletTransactionDirection.CREDIT,
                new Money(100, CurrencyCode.IRT),
                new Money(0, CurrencyCode.IRT),
                new Money(100, CurrencyCode.IRT),
                "test",
                null,
                "key-1",
                "wallet.test",
                NOW
        );
        when(transactionRepository.findByWalletIdAndIdempotencyKey(wallet.getId(), "key-1")).thenReturn(Optional.of(existing));

        assertThat(service.credit(command("key-1", 100)).outcome()).isEqualTo(WalletOperationOutcome.REPLAYED);
        assertThat(service.credit(command("key-1", 200)).outcome()).isEqualTo(WalletOperationOutcome.REJECTED_IDEMPOTENCY_CONFLICT);
        verify(transactionRepository, never()).save(any(WalletTransaction.class));
    }

    private CreditWalletCommand command(String idempotencyKey, long amount) {
        return new CreditWalletCommand(
                userId,
                new Money(amount, CurrencyCode.IRT),
                WalletTransactionType.SYSTEM_CREDIT,
                "test",
                null,
                idempotencyKey,
                "wallet.test"
        );
    }
}
