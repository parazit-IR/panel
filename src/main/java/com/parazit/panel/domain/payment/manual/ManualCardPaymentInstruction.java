package com.parazit.panel.domain.payment.manual;

import com.parazit.panel.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "manual_card_payment_instructions",
        indexes = {
                @Index(name = "idx_manual_card_instructions_payment_id", columnList = "payment_id"),
                @Index(name = "idx_manual_card_instructions_request_id", columnList = "instruction_request_id"),
                @Index(name = "idx_manual_card_instructions_status", columnList = "status"),
                @Index(name = "idx_manual_card_instructions_expires_at", columnList = "expires_at"),
                @Index(name = "idx_manual_card_instructions_amount", columnList = "currency,payable_amount")
        }
)
public class ManualCardPaymentInstruction extends BaseEntity {

    public static final int CURRENCY_MAX_LENGTH = 8;
    public static final int DESTINATION_ID_MAX_LENGTH = 64;
    public static final int BANK_NAME_MAX_LENGTH = 128;
    public static final int CARD_HOLDER_NAME_MAX_LENGTH = 128;
    public static final int CARD_NUMBER_MASKED_MAX_LENGTH = 32;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "instruction_request_id", nullable = false, updatable = false)
    private UUID instructionRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ManualPaymentInstructionStatus status;

    @Column(name = "base_amount", nullable = false, updatable = false)
    private long baseAmount;

    @Column(name = "unique_suffix_amount", nullable = false, updatable = false)
    private long uniqueSuffixAmount;

    @Column(name = "payable_amount", nullable = false, updatable = false)
    private long payableAmount;

    @Column(name = "currency", nullable = false, length = CURRENCY_MAX_LENGTH, updatable = false)
    private String currency;

    @Column(name = "destination_id", nullable = false, length = DESTINATION_ID_MAX_LENGTH, updatable = false)
    private String destinationId;

    @Column(name = "bank_name_snapshot", nullable = false, length = BANK_NAME_MAX_LENGTH, updatable = false)
    private String bankNameSnapshot;

    @Column(name = "card_holder_name_snapshot", nullable = false, length = CARD_HOLDER_NAME_MAX_LENGTH, updatable = false)
    private String cardHolderNameSnapshot;

    @Column(name = "card_number_masked_snapshot", nullable = false, length = CARD_NUMBER_MASKED_MAX_LENGTH, updatable = false)
    private String cardNumberMaskedSnapshot;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "paid_claimed_at")
    private Instant paidClaimedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    protected ManualCardPaymentInstruction() {
    }

    private ManualCardPaymentInstruction(
            UUID paymentId,
            UUID instructionRequestId,
            long baseAmount,
            long uniqueSuffixAmount,
            String currency,
            ManualPaymentDestination destination,
            Instant issuedAt,
            Duration ttl
    ) {
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId must not be null");
        this.instructionRequestId = Objects.requireNonNull(instructionRequestId, "instructionRequestId must not be null");
        this.baseAmount = requirePositive(baseAmount, "baseAmount");
        this.uniqueSuffixAmount = requirePositive(uniqueSuffixAmount, "uniqueSuffixAmount");
        this.payableAmount = Math.addExact(this.baseAmount, this.uniqueSuffixAmount);
        this.currency = normalizeCurrency(currency);
        ManualPaymentDestination requiredDestination = Objects.requireNonNull(destination, "destination must not be null");
        this.destinationId = requiredDestination.destinationId();
        this.bankNameSnapshot = requiredDestination.bankName();
        this.cardHolderNameSnapshot = requiredDestination.cardHolderName();
        this.cardNumberMaskedSnapshot = requiredDestination.maskedCardNumber();
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        Duration requiredTtl = Objects.requireNonNull(ttl, "ttl must not be null");
        if (requiredTtl.isZero() || requiredTtl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        this.expiresAt = issuedAt.plus(requiredTtl);
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
        this.status = ManualPaymentInstructionStatus.CREATED;
    }

    public static ManualCardPaymentInstruction create(
            UUID paymentId,
            UUID instructionRequestId,
            long baseAmount,
            long uniqueSuffixAmount,
            String currency,
            ManualPaymentDestination destination,
            Instant issuedAt,
            Duration ttl
    ) {
        return new ManualCardPaymentInstruction(
                paymentId,
                instructionRequestId,
                baseAmount,
                uniqueSuffixAmount,
                currency,
                destination,
                issuedAt,
                ttl
        );
    }

    public void activate() {
        if (status == ManualPaymentInstructionStatus.ACTIVE) {
            return;
        }
        requireStatus(ManualPaymentInstructionStatus.CREATED, "activate");
        status = ManualPaymentInstructionStatus.ACTIVE;
    }

    public void expire(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (status == ManualPaymentInstructionStatus.EXPIRED) {
            return;
        }
        if (status != ManualPaymentInstructionStatus.CREATED && status != ManualPaymentInstructionStatus.ACTIVE) {
            throw invalidTransition("expire");
        }
        status = ManualPaymentInstructionStatus.EXPIRED;
        expiredAt = now;
    }

    public void cancel(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (status == ManualPaymentInstructionStatus.CANCELLED) {
            return;
        }
        if (status != ManualPaymentInstructionStatus.CREATED && status != ManualPaymentInstructionStatus.ACTIVE) {
            throw invalidTransition("cancel");
        }
        status = ManualPaymentInstructionStatus.CANCELLED;
        cancelledAt = now;
    }

    public void markReceiptPending(Instant submittedAt) {
        Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        if (status == ManualPaymentInstructionStatus.RECEIPT_PENDING) {
            return;
        }
        requireStatus(ManualPaymentInstructionStatus.ACTIVE, "mark receipt pending");
        status = ManualPaymentInstructionStatus.RECEIPT_PENDING;
        paidClaimedAt = submittedAt;
    }

    public void markCompleted(Instant completedAt) {
        Objects.requireNonNull(completedAt, "completedAt must not be null");
        if (status == ManualPaymentInstructionStatus.COMPLETED) {
            return;
        }
        requireStatus(ManualPaymentInstructionStatus.RECEIPT_PENDING, "complete");
        status = ManualPaymentInstructionStatus.COMPLETED;
    }

    public boolean isActiveReservation() {
        return status == ManualPaymentInstructionStatus.CREATED
                || status == ManualPaymentInstructionStatus.ACTIVE
                || status == ManualPaymentInstructionStatus.RECEIPT_PENDING;
    }

    public boolean isExpiredAt(Instant now) {
        return isActiveReservation() && !expiresAt.isAfter(Objects.requireNonNull(now, "now must not be null"));
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getInstructionRequestId() {
        return instructionRequestId;
    }

    public ManualPaymentInstructionStatus getStatus() {
        return status;
    }

    public long getBaseAmount() {
        return baseAmount;
    }

    public long getUniqueSuffixAmount() {
        return uniqueSuffixAmount;
    }

    public long getPayableAmount() {
        return payableAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public String getBankNameSnapshot() {
        return bankNameSnapshot;
    }

    public String getCardHolderNameSnapshot() {
        return cardHolderNameSnapshot;
    }

    public String getCardNumberMaskedSnapshot() {
        return cardNumberMaskedSnapshot;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getPaidClaimedAt() {
        return paidClaimedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    private static long requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String normalizeCurrency(String value) {
        Objects.requireNonNull(value, "currency must not be null");
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
        if (normalized.length() > CURRENCY_MAX_LENGTH) {
            throw new IllegalArgumentException("currency must be at most " + CURRENCY_MAX_LENGTH + " characters");
        }
        return normalized;
    }

    private void requireStatus(ManualPaymentInstructionStatus requiredStatus, String operation) {
        if (status != requiredStatus) {
            throw invalidTransition(operation);
        }
    }

    private IllegalStateException invalidTransition(String operation) {
        return new IllegalStateException("cannot " + operation + " manual payment instruction with status " + status);
    }
}
