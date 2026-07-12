package com.parazit.panel.config.properties;

import com.parazit.panel.domain.payment.manual.BankCardNumber;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment.manual-card")
public record ManualPaymentProperties(
        boolean enabled,
        Duration instructionTtl,
        long minimumSuffix,
        long maximumSuffix,
        int maxGenerationAttempts,
        String destinationId,
        String bankName,
        String cardHolderName,
        String cardNumber,
        boolean allowInstructionReissue,
        Duration reissueCooldown
) {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    private static final Duration DEFAULT_REISSUE_COOLDOWN = Duration.ofMinutes(2);

    public ManualPaymentProperties {
        instructionTtl = defaultDuration(instructionTtl, DEFAULT_TTL, "instructionTtl");
        reissueCooldown = defaultDuration(reissueCooldown, DEFAULT_REISSUE_COOLDOWN, "reissueCooldown");
        if (minimumSuffix <= 0) {
            throw new IllegalArgumentException("minimumSuffix must be positive");
        }
        if (maximumSuffix < minimumSuffix) {
            throw new IllegalArgumentException("maximumSuffix must be greater than or equal to minimumSuffix");
        }
        if (maxGenerationAttempts <= 0 || maxGenerationAttempts > 100) {
            throw new IllegalArgumentException("maxGenerationAttempts must be between 1 and 100");
        }
        destinationId = Objects.requireNonNullElse(destinationId, "").trim();
        bankName = Objects.requireNonNullElse(bankName, "").trim();
        cardHolderName = Objects.requireNonNullElse(cardHolderName, "").trim();
        cardNumber = Objects.requireNonNullElse(cardNumber, "").trim();
        if (enabled) {
            requireText(destinationId, "destinationId");
            requireText(bankName, "bankName");
            requireText(cardHolderName, "cardHolderName");
            BankCardNumber.parse(cardNumber);
        }
    }

    @Override
    public String toString() {
        return "ManualPaymentProperties["
                + "enabled="
                + enabled
                + ", instructionTtl="
                + instructionTtl
                + ", minimumSuffix="
                + minimumSuffix
                + ", maximumSuffix="
                + maximumSuffix
                + ", maxGenerationAttempts="
                + maxGenerationAttempts
                + ", destinationId="
                + destinationId
                + ", bankName="
                + bankName
                + ", cardHolderName=<redacted>"
                + ", cardNumber=<redacted>"
                + ", allowInstructionReissue="
                + allowInstructionReissue
                + ", reissueCooldown="
                + reissueCooldown
                + ']';
    }

    private static Duration defaultDuration(Duration value, Duration fallback, String fieldName) {
        Duration duration = value == null ? fallback : value;
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return duration;
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required when manual card payment is enabled");
        }
    }
}
