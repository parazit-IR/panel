package com.parazit.panel.domain.payment.manual.receipt;

import com.parazit.panel.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Entity
@Table(
        name = "manual_payment_receipts",
        indexes = {
                @Index(name = "idx_manual_receipts_payment_id", columnList = "payment_id"),
                @Index(name = "idx_manual_receipts_instruction_id", columnList = "instruction_id"),
                @Index(name = "idx_manual_receipts_user_id", columnList = "user_id"),
                @Index(name = "idx_manual_receipts_status", columnList = "status"),
                @Index(name = "idx_manual_receipts_submitted_at", columnList = "submitted_at"),
                @Index(name = "idx_manual_receipts_review_queued_at", columnList = "review_queued_at"),
                @Index(name = "idx_manual_receipts_file_sha256", columnList = "file_sha256")
        }
)
public class ManualPaymentReceipt extends BaseEntity {

    public static final int STORAGE_PROVIDER_MAX_LENGTH = 32;
    public static final int STORAGE_KEY_MAX_LENGTH = 512;
    public static final int FILENAME_MAX_LENGTH = 255;
    public static final int CONTENT_TYPE_MAX_LENGTH = 128;
    public static final int SHA256_LENGTH = 64;
    public static final int TRACKING_NUMBER_MAX_LENGTH = 128;
    public static final int USER_NOTE_MAX_LENGTH = 1000;

    private static final Pattern SHA256_PATTERN = Pattern.compile("[a-f0-9]{64}");

    @Column(name = "receipt_request_id", nullable = false, updatable = false)
    private UUID receiptRequestId;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "instruction_id", nullable = false, updatable = false)
    private UUID instructionId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ManualPaymentReceiptStatus status;

    @Column(name = "storage_provider", length = STORAGE_PROVIDER_MAX_LENGTH)
    private String storageProvider;

    @Column(name = "storage_key", length = STORAGE_KEY_MAX_LENGTH)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = FILENAME_MAX_LENGTH, updatable = false)
    private String originalFilename;

    @Column(name = "sanitized_filename", length = FILENAME_MAX_LENGTH)
    private String sanitizedFilename;

    @Column(name = "detected_content_type", length = CONTENT_TYPE_MAX_LENGTH)
    private String detectedContentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_sha256", length = SHA256_LENGTH)
    private String fileSha256;

    @Column(name = "claimed_tracking_number", length = TRACKING_NUMBER_MAX_LENGTH)
    private String claimedTrackingNumber;

    @Column(name = "claimed_sender_card_last_four", length = 4)
    private String claimedSenderCardLastFour;

    @Column(name = "claimed_paid_at")
    private Instant claimedPaidAt;

    @Column(name = "claimed_amount", nullable = false, updatable = false)
    private long claimedAmount;

    @Column(name = "user_note", length = USER_NOTE_MAX_LENGTH)
    private String userNote;

    @Column(name = "duplicate_hash_detected", nullable = false)
    private boolean duplicateHashDetected;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "review_queued_at")
    private Instant reviewQueuedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    protected ManualPaymentReceipt() {
    }

    private ManualPaymentReceipt(
            UUID receiptRequestId,
            UUID paymentId,
            UUID instructionId,
            UUID userId,
            String originalFilename,
            long claimedAmount,
            String claimedTrackingNumber,
            String claimedSenderCardLastFour,
            Instant claimedPaidAt,
            String userNote,
            Instant submittedAt
    ) {
        this.receiptRequestId = Objects.requireNonNull(receiptRequestId, "receiptRequestId must not be null");
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId must not be null");
        this.instructionId = Objects.requireNonNull(instructionId, "instructionId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.originalFilename = requireText(originalFilename, "originalFilename", FILENAME_MAX_LENGTH);
        this.claimedAmount = requirePositive(claimedAmount, "claimedAmount");
        this.claimedTrackingNumber = normalizeOptional(claimedTrackingNumber, TRACKING_NUMBER_MAX_LENGTH);
        this.claimedSenderCardLastFour = normalizeLastFour(claimedSenderCardLastFour);
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        this.claimedPaidAt = validateClaimedPaidAt(claimedPaidAt, submittedAt);
        this.userNote = normalizeOptional(userNote, USER_NOTE_MAX_LENGTH);
        this.status = ManualPaymentReceiptStatus.UPLOADING;
    }

    public static ManualPaymentReceipt createUploading(
            UUID receiptRequestId,
            UUID paymentId,
            UUID instructionId,
            UUID userId,
            String originalFilename,
            long claimedAmount,
            String claimedTrackingNumber,
            String claimedSenderCardLastFour,
            Instant claimedPaidAt,
            String userNote,
            Instant submittedAt
    ) {
        return new ManualPaymentReceipt(
                receiptRequestId,
                paymentId,
                instructionId,
                userId,
                originalFilename,
                claimedAmount,
                claimedTrackingNumber,
                claimedSenderCardLastFour,
                claimedPaidAt,
                userNote,
                submittedAt
        );
    }

    public void markStored(
            String storageProvider,
            String storageKey,
            String sanitizedFilename,
            String detectedContentType,
            long fileSizeBytes,
            String fileSha256,
            boolean duplicateHashDetected
    ) {
        if (status == ManualPaymentReceiptStatus.SUBMITTED) {
            return;
        }
        requireStatus(ManualPaymentReceiptStatus.UPLOADING, "store");
        this.storageProvider = requireText(storageProvider, "storageProvider", STORAGE_PROVIDER_MAX_LENGTH);
        this.storageKey = requireText(storageKey, "storageKey", STORAGE_KEY_MAX_LENGTH);
        this.sanitizedFilename = requireText(sanitizedFilename, "sanitizedFilename", FILENAME_MAX_LENGTH);
        this.detectedContentType = requireText(detectedContentType, "detectedContentType", CONTENT_TYPE_MAX_LENGTH);
        this.fileSizeBytes = requirePositive(fileSizeBytes, "fileSizeBytes");
        this.fileSha256 = normalizeSha256(fileSha256);
        this.duplicateHashDetected = duplicateHashDetected;
        status = ManualPaymentReceiptStatus.SUBMITTED;
    }

    public void queueForReview(Instant reviewQueuedAt) {
        if (status == ManualPaymentReceiptStatus.QUEUED_FOR_REVIEW) {
            return;
        }
        requireStatus(ManualPaymentReceiptStatus.SUBMITTED, "queue for review");
        this.reviewQueuedAt = Objects.requireNonNull(reviewQueuedAt, "reviewQueuedAt must not be null");
        status = ManualPaymentReceiptStatus.QUEUED_FOR_REVIEW;
    }

    public void markInvalidFile(String safeReason) {
        if (status == ManualPaymentReceiptStatus.INVALID_FILE) {
            return;
        }
        requireStatus(ManualPaymentReceiptStatus.UPLOADING, "mark invalid file");
        userNote = normalizeOptional(safeReason, USER_NOTE_MAX_LENGTH);
        status = ManualPaymentReceiptStatus.INVALID_FILE;
    }

    public void withdraw(Instant withdrawnAt) {
        if (status == ManualPaymentReceiptStatus.WITHDRAWN) {
            return;
        }
        if (status != ManualPaymentReceiptStatus.SUBMITTED && status != ManualPaymentReceiptStatus.QUEUED_FOR_REVIEW) {
            throw invalidTransition("withdraw");
        }
        this.withdrawnAt = Objects.requireNonNull(withdrawnAt, "withdrawnAt must not be null");
        status = ManualPaymentReceiptStatus.WITHDRAWN;
    }

    public boolean hasStoredContent() {
        return storageKey != null && fileSha256 != null && fileSizeBytes != null;
    }

    public boolean isActiveReviewWorkflow() {
        return status == ManualPaymentReceiptStatus.UPLOADING
                || status == ManualPaymentReceiptStatus.SUBMITTED
                || status == ManualPaymentReceiptStatus.QUEUED_FOR_REVIEW;
    }

    public UUID getReceiptRequestId() {
        return receiptRequestId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getInstructionId() {
        return instructionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public ManualPaymentReceiptStatus getStatus() {
        return status;
    }

    public String getStorageProvider() {
        return storageProvider;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getSanitizedFilename() {
        return sanitizedFilename;
    }

    public String getDetectedContentType() {
        return detectedContentType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getFileSha256() {
        return fileSha256;
    }

    public String getClaimedTrackingNumber() {
        return claimedTrackingNumber;
    }

    public String getClaimedSenderCardLastFour() {
        return claimedSenderCardLastFour;
    }

    public Instant getClaimedPaidAt() {
        return claimedPaidAt;
    }

    public long getClaimedAmount() {
        return claimedAmount;
    }

    public String getUserNote() {
        return userNote;
    }

    public boolean isDuplicateHashDetected() {
        return duplicateHashDetected;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getReviewQueuedAt() {
        return reviewQueuedAt;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }

    private void requireStatus(ManualPaymentReceiptStatus expected, String operation) {
        if (status != expected) {
            throw invalidTransition(operation);
        }
    }

    private IllegalStateException invalidTransition(String operation) {
        return new IllegalStateException("cannot " + operation + " manual payment receipt with status " + status);
    }

    private static long requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters");
        }
        return normalized;
    }

    private static String normalizeOptional(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("value must be at most " + maxLength + " characters");
        }
        return normalized;
    }

    private static String normalizeLastFour(String value) {
        String normalized = normalizeOptional(value, 4);
        if (normalized == null) {
            return null;
        }
        if (!normalized.matches("\\d{4}")) {
            throw new IllegalArgumentException("claimedSenderCardLastFour must contain exactly four digits");
        }
        return normalized;
    }

    private static Instant validateClaimedPaidAt(Instant claimedPaidAt, Instant submittedAt) {
        if (claimedPaidAt == null) {
            return null;
        }
        if (claimedPaidAt.isAfter(submittedAt.plus(5, ChronoUnit.MINUTES))) {
            throw new IllegalArgumentException("claimedPaidAt cannot be in the future");
        }
        return claimedPaidAt;
    }

    private static String normalizeSha256(String value) {
        String normalized = requireText(value, "fileSha256", SHA256_LENGTH).toLowerCase(Locale.ROOT);
        if (!SHA256_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("fileSha256 must be lowercase SHA-256 hex");
        }
        return normalized;
    }
}
