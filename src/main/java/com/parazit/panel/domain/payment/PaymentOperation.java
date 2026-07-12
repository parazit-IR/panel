package com.parazit.panel.domain.payment;

import com.parazit.panel.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "payment_operations",
        indexes = {
                @Index(name = "idx_payment_operations_payment_id", columnList = "payment_id"),
                @Index(name = "idx_payment_operations_type", columnList = "operation_type"),
                @Index(name = "idx_payment_operations_occurred_at", columnList = "occurred_at")
        }
)
public class PaymentOperation extends BaseEntity {

    public static final int MESSAGE_MAX_LENGTH = 500;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 40, updatable = false)
    private PaymentOperationType operationType;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "message", length = MESSAGE_MAX_LENGTH, updatable = false)
    private String message;

    protected PaymentOperation() {
    }

    private PaymentOperation(UUID paymentId, PaymentOperationType operationType, Instant occurredAt, String message) {
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId must not be null");
        this.operationType = Objects.requireNonNull(operationType, "operationType must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.message = normalizeMessage(message);
    }

    public static PaymentOperation record(
            UUID paymentId,
            PaymentOperationType operationType,
            Instant occurredAt,
            String message
    ) {
        return new PaymentOperation(paymentId, operationType, occurredAt, message);
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public PaymentOperationType getOperationType() {
        return operationType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getMessage() {
        return message;
    }

    private static String normalizeMessage(String message) {
        if (message == null) {
            return null;
        }
        String normalized = message.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > MESSAGE_MAX_LENGTH) {
            return normalized.substring(0, MESSAGE_MAX_LENGTH);
        }
        return normalized;
    }
}
