package com.parazit.panel.application.payment.manual.receipt;

import com.parazit.panel.config.properties.PaymentReceiptStorageProperties;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PaymentReceiptFilenameSanitizer {

    private static final int MAX_FILENAME_LENGTH = 120;

    private final PaymentReceiptStorageProperties properties;

    public PaymentReceiptFilenameSanitizer(PaymentReceiptStorageProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public String sanitize(String originalFilename, String normalizedExtension) {
        Objects.requireNonNull(originalFilename, "originalFilename must not be null");
        String filename = originalFilename.replace('\\', '/');
        int slash = filename.lastIndexOf('/');
        if (slash >= 0) {
            filename = filename.substring(slash + 1);
        }
        filename = Normalizer.normalize(filename, Normalizer.Form.NFKC).trim();
        if (filename.isBlank()) {
            throw new ManualPaymentReceiptInvalidFileException("Receipt filename is blank");
        }
        if (filename.chars().anyMatch(ch -> Character.isISOControl(ch))) {
            throw new ManualPaymentReceiptInvalidFileException("Receipt filename contains control characters");
        }
        String extension = normalizeExtension(normalizedExtension);
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".exe") || lower.endsWith(".sh") || lower.endsWith(".bat") || lower.endsWith(".html") || lower.endsWith(".svg")) {
            throw new ManualPaymentReceiptUnsupportedTypeException("Receipt filename extension is not supported");
        }
        int dot = filename.lastIndexOf('.');
        String base = dot > 0 ? filename.substring(0, dot) : filename;
        base = base.replaceAll("[^A-Za-z0-9._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^[._-]+", "")
                .replaceAll("[._-]+$", "");
        if (base.isBlank()) {
            base = "receipt";
        }
        int maxBaseLength = MAX_FILENAME_LENGTH - extension.length() - 1;
        if (base.length() > maxBaseLength) {
            base = base.substring(0, maxBaseLength);
        }
        return base + "." + extension;
    }

    private String normalizeExtension(String extension) {
        String normalized = Objects.requireNonNull(extension, "extension must not be null")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        if (!properties.allowedExtensions().contains(normalized)) {
            throw new ManualPaymentReceiptUnsupportedTypeException("Receipt file extension is not supported");
        }
        return normalized;
    }
}
