package com.parazit.panel.domain.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.plan.CurrencyCode;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WalletTest {

    @Test
    void startsActiveWithZeroBalance() {
        UUID userId = UUID.randomUUID();

        Wallet wallet = Wallet.create(userId, CurrencyCode.IRT);

        assertThat(wallet.getUserId()).isEqualTo(userId);
        assertThat(wallet.balance()).isEqualTo(new Money(0, CurrencyCode.IRT));
        assertThat(wallet.getStatus()).isEqualTo(WalletStatus.ACTIVE);
    }

    @Test
    void creditsAndDebitsWithoutNegativeBalance() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), CurrencyCode.IRT);

        assertThat(wallet.credit(new Money(500_000, CurrencyCode.IRT))).isEqualTo(new Money(500_000, CurrencyCode.IRT));
        assertThat(wallet.debit(new Money(200_000, CurrencyCode.IRT))).isEqualTo(new Money(300_000, CurrencyCode.IRT));
        assertThat(wallet.debit(new Money(300_000, CurrencyCode.IRT))).isEqualTo(new Money(0, CurrencyCode.IRT));
        assertThatThrownBy(() -> wallet.debit(new Money(1, CurrencyCode.IRT)))
                .isInstanceOf(InsufficientWalletBalanceException.class);
        assertThat(wallet.balance()).isEqualTo(new Money(0, CurrencyCode.IRT));
    }

    @Test
    void rejectsInvalidAmountsAndClosedOrLockedWallets() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), CurrencyCode.IRT);

        assertThatThrownBy(() -> wallet.credit(new Money(0, CurrencyCode.IRT)))
                .isInstanceOf(InvalidWalletAmountException.class);
        wallet.lock();
        assertThatThrownBy(() -> wallet.credit(new Money(1, CurrencyCode.IRT)))
                .isInstanceOf(WalletLockedException.class);

        Wallet closed = Wallet.create(UUID.randomUUID(), CurrencyCode.IRT);
        closed.close();
        assertThatThrownBy(() -> closed.debit(new Money(1, CurrencyCode.IRT)))
                .isInstanceOf(WalletClosedException.class);
    }

    @Test
    void protectsAgainstOverflow() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), CurrencyCode.IRT);
        wallet.credit(new Money(Long.MAX_VALUE, CurrencyCode.IRT));

        assertThatThrownBy(() -> wallet.credit(new Money(1, CurrencyCode.IRT)))
                .isInstanceOf(ArithmeticException.class);
    }
}
