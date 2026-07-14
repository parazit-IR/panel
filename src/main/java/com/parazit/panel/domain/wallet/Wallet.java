package com.parazit.panel.domain.wallet;

import com.parazit.panel.common.persistence.BaseEntity;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.plan.CurrencyCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "wallets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_wallets_user_id", columnNames = "user_id")
        },
        indexes = {
                @Index(name = "idx_wallets_user_id", columnList = "user_id"),
                @Index(name = "idx_wallets_status", columnList = "status")
        }
)
public class Wallet extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "currency", nullable = false, length = 8, updatable = false)
    private String currency;

    @Column(name = "balance", nullable = false)
    private long balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WalletStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Wallet() {
    }

    private Wallet(UUID userId, CurrencyCode currency) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.currency = Objects.requireNonNull(currency, "currency must not be null").name();
        this.balance = 0;
        this.status = WalletStatus.ACTIVE;
    }

    public static Wallet create(UUID userId, CurrencyCode currency) {
        return new Wallet(userId, currency);
    }

    public Money balance() {
        return new Money(balance, currencyCode());
    }

    public Money credit(Money amount) {
        requireActive();
        long value = requirePositiveSameCurrency(amount);
        balance = Math.addExact(balance, value);
        return balance();
    }

    public Money debit(Money amount) {
        requireActive();
        long value = requirePositiveSameCurrency(amount);
        if (balance < value) {
            throw new InsufficientWalletBalanceException();
        }
        balance -= value;
        return balance();
    }

    public void lock() {
        if (status == WalletStatus.CLOSED) {
            throw new IllegalStateException("cannot lock closed wallet");
        }
        status = WalletStatus.LOCKED;
    }

    public void close() {
        status = WalletStatus.CLOSED;
    }

    public UUID getUserId() {
        return userId;
    }

    public long getBalanceAmount() {
        return balance;
    }

    public CurrencyCode currencyCode() {
        return CurrencyCode.valueOf(currency);
    }

    public String getCurrency() {
        return currency;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    private void requireActive() {
        if (status == WalletStatus.LOCKED) {
            throw new WalletLockedException();
        }
        if (status == WalletStatus.CLOSED) {
            throw new WalletClosedException();
        }
        if (status != WalletStatus.ACTIVE) {
            throw new IllegalStateException("wallet is not active");
        }
    }

    private long requirePositiveSameCurrency(Money amount) {
        Money requiredAmount = Objects.requireNonNull(amount, "amount must not be null");
        if (requiredAmount.amount() <= 0) {
            throw new InvalidWalletAmountException();
        }
        if (requiredAmount.currency() != currencyCode()) {
            throw new WalletCurrencyMismatchException();
        }
        return requiredAmount.amount();
    }

    @Override
    public String toString() {
        return "Wallet[id=" + getId()
                + ", userId=" + userId
                + ", currency=" + currency
                + ", status=" + status
                + "]";
    }
}
