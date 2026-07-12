package com.parazit.panel.domain.payment.zarinpal;

import com.parazit.panel.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "zarinpal_payment_attempts",
        indexes = {
                @Index(name = "idx_zarinpal_attempts_payment_id", columnList = "payment_id"),
                @Index(name = "idx_zarinpal_attempts_request_id", columnList = "request_id"),
                @Index(name = "idx_zarinpal_attempts_authority", columnList = "authority"),
                @Index(name = "idx_zarinpal_attempts_reference_id", columnList = "reference_id"),
                @Index(name = "idx_zarinpal_attempts_status", columnList = "status"),
                @Index(name = "idx_zarinpal_attempts_created_at", columnList = "created_at")
        }
)
public class ZarinpalPaymentAttempt extends BaseEntity {

    public static final int AUTHORITY_MAX_LENGTH = 64;
    public static final int REFERENCE_ID_MAX_LENGTH = 128;
    public static final int GATEWAY_CODE_MAX_LENGTH = 32;
    public static final int CARD_HASH_MAX_LENGTH = 128;
    public static final int CARD_PAN_MASKED_MAX_LENGTH = 32;
    public static final int FAILURE_MESSAGE_MAX_LENGTH = 500;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ZarinpalAttemptStatus status;

    @Column(name = "gateway_amount", nullable = false, updatable = false)
    private long gatewayAmount;

    @Column(name = "authority", length = AUTHORITY_MAX_LENGTH)
    private String authority;

    @Column(name = "reference_id", length = REFERENCE_ID_MAX_LENGTH)
    private String referenceId;

    @Column(name = "gateway_code", length = GATEWAY_CODE_MAX_LENGTH)
    private String gatewayCode;

    @Column(name = "card_hash", length = CARD_HASH_MAX_LENGTH)
    private String cardHash;

    @Column(name = "card_pan_masked", length = CARD_PAN_MASKED_MAX_LENGTH)
    private String cardPanMasked;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "redirected_at")
    private Instant redirectedAt;

    @Column(name = "callback_received_at")
    private Instant callbackReceivedAt;

    @Column(name = "verification_started_at")
    private Instant verificationStartedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "failure_message", length = FAILURE_MESSAGE_MAX_LENGTH)
    private String failureMessage;

    protected ZarinpalPaymentAttempt() {
    }

    private ZarinpalPaymentAttempt(UUID paymentId, UUID requestId, long gatewayAmount) {
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId must not be null");
        this.requestId = Objects.requireNonNull(requestId, "requestId must not be null");
        this.gatewayAmount = requirePositive(gatewayAmount, "gatewayAmount");
        this.status = ZarinpalAttemptStatus.CREATED;
    }

    public static ZarinpalPaymentAttempt create(UUID paymentId, UUID requestId, long gatewayAmount) {
        return new ZarinpalPaymentAttempt(paymentId, requestId, gatewayAmount);
    }

    public void markRequesting(Instant requestedAt) {
        if (status == ZarinpalAttemptStatus.REQUESTING) {
            return;
        }
        requireStatus(ZarinpalAttemptStatus.CREATED, "request");
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        status = ZarinpalAttemptStatus.REQUESTING;
        clearFailure();
    }

    public void markRedirectReady(String authority, Instant redirectedAt, String gatewayCode) {
        if (status == ZarinpalAttemptStatus.REDIRECT_READY) {
            return;
        }
        if (status != ZarinpalAttemptStatus.REQUESTING && status != ZarinpalAttemptStatus.UNKNOWN) {
            throw invalidTransition("prepare redirect");
        }
        this.authority = normalizeAuthority(authority);
        this.redirectedAt = Objects.requireNonNull(redirectedAt, "redirectedAt must not be null");
        this.gatewayCode = normalizeOptional(gatewayCode, "gatewayCode", GATEWAY_CODE_MAX_LENGTH);
        status = ZarinpalAttemptStatus.REDIRECT_READY;
        clearFailure();
    }

    public void markCallbackReceived(Instant callbackReceivedAt) {
        if (status == ZarinpalAttemptStatus.CALLBACK_RECEIVED) {
            return;
        }
        if (status == ZarinpalAttemptStatus.VERIFIED || status == ZarinpalAttemptStatus.CANCELLED) {
            return;
        }
        if (status != ZarinpalAttemptStatus.REDIRECT_READY
                && status != ZarinpalAttemptStatus.UNKNOWN
                && status != ZarinpalAttemptStatus.FAILED) {
            throw invalidTransition("receive callback");
        }
        this.callbackReceivedAt = Objects.requireNonNull(callbackReceivedAt, "callbackReceivedAt must not be null");
        status = ZarinpalAttemptStatus.CALLBACK_RECEIVED;
    }

    public void markVerifying(Instant verificationStartedAt) {
        if (status == ZarinpalAttemptStatus.VERIFYING) {
            return;
        }
        if (status == ZarinpalAttemptStatus.UNKNOWN) {
            this.verificationStartedAt = Objects.requireNonNull(verificationStartedAt, "verificationStartedAt must not be null");
            status = ZarinpalAttemptStatus.VERIFYING;
            return;
        }
        if (status != ZarinpalAttemptStatus.CALLBACK_RECEIVED && status != ZarinpalAttemptStatus.REDIRECT_READY) {
            throw invalidTransition("verify");
        }
        this.verificationStartedAt = Objects.requireNonNull(verificationStartedAt, "verificationStartedAt must not be null");
        status = ZarinpalAttemptStatus.VERIFYING;
    }

    public void markVerified(
            String referenceId,
            String gatewayCode,
            String cardHash,
            String cardPanMasked,
            Instant verifiedAt
    ) {
        if (status == ZarinpalAttemptStatus.VERIFIED) {
            return;
        }
        if (status != ZarinpalAttemptStatus.VERIFYING && status != ZarinpalAttemptStatus.UNKNOWN) {
            throw invalidTransition("mark verified");
        }
        this.referenceId = requireText(referenceId, "referenceId", REFERENCE_ID_MAX_LENGTH);
        this.gatewayCode = normalizeOptional(gatewayCode, "gatewayCode", GATEWAY_CODE_MAX_LENGTH);
        this.cardHash = normalizeOptional(cardHash, "cardHash", CARD_HASH_MAX_LENGTH);
        this.cardPanMasked = normalizeOptional(cardPanMasked, "cardPanMasked", CARD_PAN_MASKED_MAX_LENGTH);
        this.verifiedAt = Objects.requireNonNull(verifiedAt, "verifiedAt must not be null");
        status = ZarinpalAttemptStatus.VERIFIED;
        clearFailure();
    }

    public void markCancelled(Instant cancelledAt) {
        Objects.requireNonNull(cancelledAt, "cancelledAt must not be null");
        if (status == ZarinpalAttemptStatus.CANCELLED || status == ZarinpalAttemptStatus.VERIFIED) {
            return;
        }
        if (status != ZarinpalAttemptStatus.REDIRECT_READY
                && status != ZarinpalAttemptStatus.CALLBACK_RECEIVED
                && status != ZarinpalAttemptStatus.UNKNOWN) {
            throw invalidTransition("cancel");
        }
        callbackReceivedAt = cancelledAt;
        status = ZarinpalAttemptStatus.CANCELLED;
    }

    public void markFailed(String failureCode, String failureMessage, Instant failedAt) {
        if (status == ZarinpalAttemptStatus.FAILED) {
            return;
        }
        if (status == ZarinpalAttemptStatus.VERIFIED) {
            return;
        }
        this.failureCode = normalizeOptional(failureCode, "failureCode", 64);
        this.failureMessage = normalizeOptional(failureMessage, "failureMessage", FAILURE_MESSAGE_MAX_LENGTH);
        this.failedAt = Objects.requireNonNull(failedAt, "failedAt must not be null");
        status = ZarinpalAttemptStatus.FAILED;
    }

    public void markUnknown(String failureCode, String failureMessage, Instant unknownAt) {
        Objects.requireNonNull(unknownAt, "unknownAt must not be null");
        if (status == ZarinpalAttemptStatus.VERIFIED) {
            return;
        }
        this.failureCode = normalizeOptional(failureCode, "failureCode", 64);
        this.failureMessage = normalizeOptional(failureMessage, "failureMessage", FAILURE_MESSAGE_MAX_LENGTH);
        status = ZarinpalAttemptStatus.UNKNOWN;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public ZarinpalAttemptStatus getStatus() {
        return status;
    }

    public long getGatewayAmount() {
        return gatewayAmount;
    }

    public String getAuthority() {
        return authority;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getGatewayCode() {
        return gatewayCode;
    }

    public String getCardHash() {
        return cardHash;
    }

    public String getCardPanMasked() {
        return cardPanMasked;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getRedirectedAt() {
        return redirectedAt;
    }

    public Instant getCallbackReceivedAt() {
        return callbackReceivedAt;
    }

    public Instant getVerificationStartedAt() {
        return verificationStartedAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public static String normalizeAuthority(String authority) {
        return requireText(authority, "authority", AUTHORITY_MAX_LENGTH);
    }

    private void requireStatus(ZarinpalAttemptStatus expected, String action) {
        if (status != expected) {
            throw invalidTransition(action);
        }
    }

    private IllegalStateException invalidTransition(String action) {
        return new IllegalStateException("cannot " + action + " Zarinpal attempt with status " + status);
    }

    private void clearFailure() {
        failureCode = null;
        failureMessage = null;
        failedAt = null;
    }

    private static long requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        String normalized = normalizeOptional(value, fieldName, maxLength);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeOptional(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            normalized = normalized.substring(0, maxLength);
        }
        if ("gatewayCode".equals(fieldName) || "failureCode".equals(fieldName)) {
            return normalized.toUpperCase(Locale.ROOT);
        }
        return normalized;
    }
}
