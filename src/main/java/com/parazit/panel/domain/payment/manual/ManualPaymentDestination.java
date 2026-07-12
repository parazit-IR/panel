package com.parazit.panel.domain.payment.manual;

import java.util.Objects;

public record ManualPaymentDestination(
        String destinationId,
        String bankName,
        String cardHolderName,
        BankCardNumber cardNumber,
        boolean active,
        int displayOrder
) {

    public ManualPaymentDestination {
        destinationId = requireText(destinationId, "destinationId", 64);
        bankName = requireText(bankName, "bankName", 128);
        cardHolderName = requireText(cardHolderName, "cardHolderName", 128);
        cardNumber = Objects.requireNonNull(cardNumber, "cardNumber must not be null");
        if (displayOrder < 0) {
            throw new IllegalArgumentException("displayOrder must be zero or positive");
        }
    }

    public String maskedCardNumber() {
        return cardNumber.masked();
    }

    public String formattedCardNumber() {
        return cardNumber.formatted();
    }

    @Override
    public String toString() {
        return "ManualPaymentDestination["
                + "destinationId="
                + destinationId
                + ", bankName="
                + bankName
                + ", cardHolderName=<redacted>"
                + ", cardNumber="
                + cardNumber.masked()
                + ", active="
                + active
                + ", displayOrder="
                + displayOrder
                + ']';
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
}
