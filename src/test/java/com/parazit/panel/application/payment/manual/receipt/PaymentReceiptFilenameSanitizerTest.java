package com.parazit.panel.application.payment.manual.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.config.properties.PaymentReceiptStorageProperties;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PaymentReceiptFilenameSanitizerTest {

    private final PaymentReceiptFilenameSanitizer sanitizer = new PaymentReceiptFilenameSanitizer(properties());

    @Test
    void stripsDirectoryComponentsAndNormalizesUnsafeCharacters() {
        assertThat(sanitizer.sanitize("../../bank receipt 01.png", "png"))
                .isEqualTo("bank_receipt_01.png");
        assertThat(sanitizer.sanitize("C:\\temp\\receipt-final.JPG", "jpg"))
                .isEqualTo("receipt-final.jpg");
        assertThat(sanitizer.sanitize("رسید پرداخت.png", "png"))
                .isEqualTo("receipt.png");
    }

    @Test
    void rejectsBlankControlAndExecutableNames() {
        assertThatThrownBy(() -> sanitizer.sanitize("   ", "png"))
                .isInstanceOf(ManualPaymentReceiptInvalidFileException.class);
        assertThatThrownBy(() -> sanitizer.sanitize("receipt\u0000.png", "png"))
                .isInstanceOf(ManualPaymentReceiptInvalidFileException.class);
        assertThatThrownBy(() -> sanitizer.sanitize("receipt.jpg.exe", "jpg"))
                .isInstanceOf(ManualPaymentReceiptUnsupportedTypeException.class);
    }

    @Test
    void limitsLongNamesAndUsesDetectedExtension() {
        String sanitized = sanitizer.sanitize("a".repeat(180) + ".jpeg", "jpg");

        assertThat(sanitized)
                .endsWith(".jpg")
                .hasSizeLessThanOrEqualTo(120);
    }

    @Test
    void rejectsUnsupportedDetectedExtension() {
        assertThatThrownBy(() -> sanitizer.sanitize("receipt.png", "exe"))
                .isInstanceOf(ManualPaymentReceiptUnsupportedTypeException.class);
    }

    private static PaymentReceiptStorageProperties properties() {
        return new PaymentReceiptStorageProperties(
                true,
                "local",
                Path.of("/tmp/panel-test-receipts"),
                5 * 1024 * 1024,
                Set.of("image/jpeg", "image/png", "application/pdf"),
                Set.of("jpg", "jpeg", "png", "pdf"),
                true,
                5000,
                5000,
                Duration.ofSeconds(30),
                true
        );
    }
}
