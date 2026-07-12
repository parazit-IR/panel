package com.parazit.panel.config.properties;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment.receipt-storage")
public record PaymentReceiptStorageProperties(
        boolean enabled,
        String provider,
        Path localRoot,
        long maxFileSizeBytes,
        Set<String> allowedContentTypes,
        Set<String> allowedExtensions,
        boolean allowPdf,
        int imageMaxWidth,
        int imageMaxHeight,
        Duration uploadTimeout,
        boolean calculateSha256
) {

    private static final long DEFAULT_MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;

    public PaymentReceiptStorageProperties {
        provider = normalizeProvider(provider);
        localRoot = localRoot == null ? Path.of("/tmp/panel/manual-payment-receipts") : localRoot.toAbsolutePath().normalize();
        maxFileSizeBytes = maxFileSizeBytes <= 0 ? DEFAULT_MAX_FILE_SIZE_BYTES : maxFileSizeBytes;
        if (maxFileSizeBytes > 25L * 1024L * 1024L) {
            throw new IllegalArgumentException("maxFileSizeBytes must be at most 25 MiB");
        }
        allowedContentTypes = normalizeSet(allowedContentTypes, Set.of("image/jpeg", "image/png", "application/pdf"));
        allowedExtensions = normalizeSet(allowedExtensions, Set.of("jpg", "jpeg", "png", "pdf"));
        if (!allowPdf) {
            allowedContentTypes = allowedContentTypes.stream()
                    .filter(value -> !value.equals("application/pdf"))
                    .collect(Collectors.toUnmodifiableSet());
            allowedExtensions = allowedExtensions.stream()
                    .filter(value -> !value.equals("pdf"))
                    .collect(Collectors.toUnmodifiableSet());
        }
        imageMaxWidth = imageMaxWidth <= 0 ? 5000 : imageMaxWidth;
        imageMaxHeight = imageMaxHeight <= 0 ? 5000 : imageMaxHeight;
        uploadTimeout = uploadTimeout == null ? Duration.ofSeconds(30) : uploadTimeout;
        if (uploadTimeout.isZero() || uploadTimeout.isNegative()) {
            throw new IllegalArgumentException("uploadTimeout must be positive");
        }
        if (isUnsafeRoot(localRoot)) {
            throw new IllegalArgumentException("localRoot must not be inside source or build directories");
        }
    }

    private static String normalizeProvider(String provider) {
        String normalized = Objects.requireNonNullElse(provider, "local").trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("local")) {
            throw new IllegalArgumentException("Only local receipt storage is supported");
        }
        return normalized;
    }

    private static Set<String> normalizeSet(Set<String> values, Set<String> fallback) {
        Set<String> source = values == null || values.isEmpty() ? fallback : values;
        return source.stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean isUnsafeRoot(Path root) {
        String normalized = root.toString().replace('\\', '/');
        return normalized.endsWith("/src")
                || normalized.contains("/src/")
                || normalized.endsWith("/build")
                || normalized.contains("/build/")
                || normalized.endsWith("/target")
                || normalized.contains("/target/");
    }
}
