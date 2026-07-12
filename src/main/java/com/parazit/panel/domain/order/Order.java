package com.parazit.panel.domain.order;

import com.parazit.panel.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_user_id", columnList = "user_id"),
                @Index(name = "idx_orders_status", columnList = "status"),
                @Index(name = "idx_orders_created_at", columnList = "created_at")
        }
)
public class Order extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "amount", nullable = false, updatable = false)
    private long amount;

    @Column(name = "currency", nullable = false, length = 8, updatable = false)
    private String currency;

    protected Order() {
    }

    private Order(UUID userId, long amount, String currency) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.amount = requirePositiveOrZero(amount, "amount");
        this.currency = normalizeCurrency(currency);
        this.status = OrderStatus.CREATED;
    }

    public static Order create(UUID userId, long amount, String currency) {
        return new Order(userId, amount, currency);
    }

    public void cancel() {
        if (status == OrderStatus.CANCELLED) {
            return;
        }
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("cannot cancel order with status " + status);
        }
        status = OrderStatus.CANCELLED;
    }

    public UUID getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    private static long requirePositiveOrZero(long value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or positive");
        }
        return value;
    }

    private static String normalizeCurrency(String currency) {
        Objects.requireNonNull(currency, "currency must not be null");
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
        if (normalized.length() > 8) {
            throw new IllegalArgumentException("currency must be at most 8 characters");
        }
        return normalized;
    }
}
