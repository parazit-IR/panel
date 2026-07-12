package com.parazit.panel.domain.payment.manual.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ManualPaymentReceiptTest {

    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");
    private static final String SHA256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void createsStoresAndQueuesForReview() {
        ManualPaymentReceipt receipt = receipt();

        assertThat(receipt.getStatus()).isEqualTo(ManualPaymentReceiptStatus.UPLOADING);
        assertThat(receipt.getOriginalFilename()).isEqualTo("receipt.png");
        assertThat(receipt.getClaimedTrackingNumber()).isEqualTo("TRK-1");
        assertThat(receipt.getClaimedSenderCardLastFour()).isEqualTo("1234");
        assertThat(receipt.getClaimedAmount()).isEqualTo(101_638L);
        assertThat(receipt.hasStoredContent()).isFalse();

        receipt.markStored("local", "manual-receipts/2026/07/id/file.png", "receipt.png", "image/png", 128, SHA256, true);
        assertThat(receipt.getStatus()).isEqualTo(ManualPaymentReceiptStatus.SUBMITTED);
        assertThat(receipt.hasStoredContent()).isTrue();
        assertThat(receipt.isDuplicateHashDetected()).isTrue();

        receipt.queueForReview(NOW.plusSeconds(2));
        assertThat(receipt.getStatus()).isEqualTo(ManualPaymentReceiptStatus.QUEUED_FOR_REVIEW);
        assertThat(receipt.getReviewQueuedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(receipt.isActiveReviewWorkflow()).isTrue();
    }

    @Test
    void validatesClaimedMetadata() {
        assertThatThrownBy(() -> ManualPaymentReceipt.createUploading(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "receipt.png",
                0,
                null,
                null,
                null,
                null,
                NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("claimedAmount");

        assertThatThrownBy(() -> ManualPaymentReceipt.createUploading(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "receipt.png",
                101_638,
                null,
                "12ab",
                null,
                null,
                NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("four digits");

        assertThatThrownBy(() -> ManualPaymentReceipt.createUploading(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "receipt.png",
                101_638,
                null,
                null,
                NOW.plusSeconds(301),
                null,
                NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    void rejectsInvalidTransitionsAndStorageMetadata() {
        ManualPaymentReceipt receipt = receipt();

        assertThatThrownBy(() -> receipt.queueForReview(NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UPLOADING");

        assertThatThrownBy(() -> receipt.markStored("local", "key", "receipt.png", "image/png", 0, SHA256, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fileSizeBytes");

        assertThatThrownBy(() -> receipt.markStored("local", "key", "receipt.png", "image/png", 1, "not-a-hash", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SHA-256");

        receipt.markInvalidFile("unsupported type");
        assertThat(receipt.getStatus()).isEqualTo(ManualPaymentReceiptStatus.INVALID_FILE);

        assertThatThrownBy(() -> receipt.markStored("local", "key", "receipt.png", "image/png", 1, SHA256, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INVALID_FILE");
    }

    @Test
    void supportsWithdrawalAfterQueueWithoutRemovingStorageMetadata() {
        ManualPaymentReceipt receipt = receipt();
        receipt.markStored("local", "manual-receipts/2026/07/id/file.png", "receipt.png", "image/png", 128, SHA256, false);
        receipt.queueForReview(NOW.plusSeconds(1));

        receipt.withdraw(NOW.plusSeconds(10));

        assertThat(receipt.getStatus()).isEqualTo(ManualPaymentReceiptStatus.WITHDRAWN);
        assertThat(receipt.getWithdrawnAt()).isEqualTo(NOW.plusSeconds(10));
        assertThat(receipt.hasStoredContent()).isTrue();
        assertThat(receipt.isActiveReviewWorkflow()).isFalse();
    }

    private ManualPaymentReceipt receipt() {
        return ManualPaymentReceipt.createUploading(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                " receipt.png ",
                101_638L,
                " TRK-1 ",
                "1234",
                NOW.minusSeconds(60),
                " user note ",
                NOW
        );
    }
}
