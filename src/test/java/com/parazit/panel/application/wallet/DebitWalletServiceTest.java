package com.parazit.panel.application.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parazit.panel.application.wallet.command.DebitWalletCommand;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.wallet.Wallet;
import com.parazit.panel.domain.wallet.WalletOperationOutcome;
import com.parazit.panel.domain.wallet.WalletTransaction;
import com.parazit.panel.domain.wallet.WalletTransactionType;
import com.parazit.panel.domain.wallet.repository.WalletRepository;
import com.parazit.panel.domain.wallet.repository.WalletTransactionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DebitWalletServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-14T00:00:00Z");

    private WalletRepository walletRepository;
    private WalletTransactionRepository transactionRepository;
    private DebitWalletService service;
    private UUID userId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        walletRepository = org.mockito.Mockito.mock(WalletRepository.class);
        transactionRepository = org.mockito.Mockito.mock(WalletTransactionRepository.class);
        service = new DebitWalletService(walletRepository, transactionRepository, () -> NOW);
        userId = UUID.randomUUID();
        wallet = Wallet.create(userId, CurrencyCode.IRT);
        wallet.credit(new Money(100, CurrencyCode.IRT));
        ReflectionTestUtils.setField(wallet, "id", UUID.randomUUID());
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(WalletTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void appliesDebitWhenBalanceIsSufficient() {
        when(transactionRepository.findByWalletIdAndIdempotencyKey(wallet.getId(), "key-1")).thenReturn(Optional.empty());

        var result = service.debit(command("key-1", 80));

        assertThat(result.outcome()).isEqualTo(WalletOperationOutcome.APPLIED);
        assertThat(result.balanceBefore()).isEqualTo(new Money(100, CurrencyCode.IRT));
        assertThat(result.balanceAfter()).isEqualTo(new Money(20, CurrencyCode.IRT));
        assertThat(wallet.balance()).isEqualTo(new Money(20, CurrencyCode.IRT));
        verify(transactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void rejectsInsufficientBalanceWithoutLedgerEntry() {
        when(transactionRepository.findByWalletIdAndIdempotencyKey(wallet.getId(), "key-1")).thenReturn(Optional.empty());

        var result = service.debit(command("key-1", 101));

        assertThat(result.outcome()).isEqualTo(WalletOperationOutcome.REJECTED_INSUFFICIENT_BALANCE);
        assertThat(wallet.balance()).isEqualTo(new Money(100, CurrencyCode.IRT));
        verify(transactionRepository, never()).save(any(WalletTransaction.class));
    }

    private DebitWalletCommand command(String idempotencyKey, long amount) {
        return new DebitWalletCommand(
                userId,
                new Money(amount, CurrencyCode.IRT),
                WalletTransactionType.SYSTEM_DEBIT,
                "test",
                null,
                idempotencyKey,
                "wallet.test"
        );
    }
}
