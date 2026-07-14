package com.parazit.panel.domain.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.plan.CurrencyCode;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WalletTransactionTest {

    private static final Instant NOW = Instant.parse("2026-07-14T00:00:00Z");

    @Test
    void postsCreditWithImmutableBeforeAfterSnapshot() {
        WalletTransaction transaction = transaction(
                WalletTransactionDirection.CREDIT,
                new Money(100, CurrencyCode.IRT),
                new Money(50, CurrencyCode.IRT),
                new Money(150, CurrencyCode.IRT)
        );

        assertThat(transaction.getStatus()).isEqualTo(WalletTransactionStatus.POSTED);
        assertThat(transaction.amount()).isEqualTo(new Money(100, CurrencyCode.IRT));
        assertThat(transaction.balanceBefore()).isEqualTo(new Money(50, CurrencyCode.IRT));
        assertThat(transaction.balanceAfter()).isEqualTo(new Money(150, CurrencyCode.IRT));
        assertThat(transaction.toString()).doesNotContain("idem-key").doesNotContain("reference");
    }

    @Test
    void postsDebitAndRejectsInvalidEquation() {
        WalletTransaction transaction = transaction(
                WalletTransactionDirection.DEBIT,
                new Money(40, CurrencyCode.IRT),
                new Money(100, CurrencyCode.IRT),
                new Money(60, CurrencyCode.IRT)
        );

        assertThat(transaction.getDirection()).isEqualTo(WalletTransactionDirection.DEBIT);
        assertThatThrownBy(() -> transaction(
                WalletTransactionDirection.DEBIT,
                new Money(80, CurrencyCode.IRT),
                new Money(100, CurrencyCode.IRT),
                new Money(30, CurrencyCode.IRT)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("equation");
    }

    @Test
    void detectsSemanticReplayMatches() {
        WalletTransaction transaction = transaction(
                WalletTransactionDirection.CREDIT,
                new Money(100, CurrencyCode.IRT),
                new Money(50, CurrencyCode.IRT),
                new Money(150, CurrencyCode.IRT)
        );

        assertThat(transaction.semanticallyMatches(
                WalletTransactionType.SYSTEM_CREDIT,
                WalletTransactionDirection.CREDIT,
                new Money(100, CurrencyCode.IRT),
                "test",
                null,
                "wallet.test"
        )).isTrue();
        assertThat(transaction.semanticallyMatches(
                WalletTransactionType.SYSTEM_CREDIT,
                WalletTransactionDirection.CREDIT,
                new Money(101, CurrencyCode.IRT),
                "test",
                null,
                "wallet.test"
        )).isFalse();
    }

    private static WalletTransaction transaction(
            WalletTransactionDirection direction,
            Money amount,
            Money before,
            Money after
    ) {
        return WalletTransaction.post(
                UUID.randomUUID(),
                UUID.randomUUID(),
                WalletTransactionType.SYSTEM_CREDIT,
                direction,
                amount,
                before,
                after,
                "test",
                null,
                "idem-key",
                "wallet.test",
                NOW
        );
    }
}
