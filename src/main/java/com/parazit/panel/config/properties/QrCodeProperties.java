package com.parazit.panel.config.properties;

import com.parazit.panel.application.qrcode.model.QrErrorCorrection;
import com.parazit.panel.application.qrcode.model.QrImageFormat;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.subscription.qr")
public record QrCodeProperties(
        boolean enabled,
        int defaultSize,
        int minSize,
        int maxSize,
        int defaultMarginModules,
        int maxMarginModules,
        QrErrorCorrection defaultErrorCorrection,
        QrImageFormat defaultFormat,
        boolean allowSvg,
        boolean allowTransparentBackground,
        int maxPayloadCharacters,
        Duration generationTimeout,
        int maxOutputBytes
) {

    public QrCodeProperties {
        minSize = minSize <= 0 ? 128 : minSize;
        maxSize = maxSize <= 0 ? 2048 : maxSize;
        defaultSize = defaultSize <= 0 ? 512 : defaultSize;
        defaultMarginModules = defaultMarginModules < 0 ? 4 : defaultMarginModules;
        maxMarginModules = maxMarginModules <= 0 ? 16 : maxMarginModules;
        defaultErrorCorrection = defaultErrorCorrection == null ? QrErrorCorrection.MEDIUM : defaultErrorCorrection;
        defaultFormat = defaultFormat == null ? QrImageFormat.PNG : defaultFormat;
        maxPayloadCharacters = maxPayloadCharacters <= 0 ? 4096 : maxPayloadCharacters;
        generationTimeout = generationTimeout == null ? Duration.ofSeconds(2) : generationTimeout;
        maxOutputBytes = maxOutputBytes <= 0 ? 1_048_576 : maxOutputBytes;

        if (minSize < 1 || minSize > maxSize) {
            throw new IllegalArgumentException("app.subscription.qr.min-size must be positive and <= max-size");
        }
        if (maxSize > 2048) {
            throw new IllegalArgumentException("app.subscription.qr.max-size must be <= 2048");
        }
        if (defaultSize < minSize || defaultSize > maxSize) {
            throw new IllegalArgumentException("app.subscription.qr.default-size must be inside configured bounds");
        }
        if (maxMarginModules > 16) {
            throw new IllegalArgumentException("app.subscription.qr.max-margin-modules must be <= 16");
        }
        if (defaultMarginModules > maxMarginModules) {
            throw new IllegalArgumentException("app.subscription.qr.default-margin-modules must be <= max-margin-modules");
        }
        if (defaultFormat == QrImageFormat.SVG && !allowSvg) {
            throw new IllegalArgumentException("default SVG QR format requires SVG to be enabled");
        }
        if (allowTransparentBackground) {
            throw new IllegalArgumentException("transparent QR background is not supported in Task 34");
        }
        if (maxPayloadCharacters < 1 || maxPayloadCharacters > 16_384) {
            throw new IllegalArgumentException("app.subscription.qr.max-payload-characters must be between 1 and 16384");
        }
        if (generationTimeout.isZero() || generationTimeout.isNegative()) {
            throw new IllegalArgumentException("app.subscription.qr.generation-timeout must be positive");
        }
        if (maxOutputBytes < 1024 || maxOutputBytes > 8_388_608) {
            throw new IllegalArgumentException("app.subscription.qr.max-output-bytes must be between 1024 and 8388608");
        }
    }
}

