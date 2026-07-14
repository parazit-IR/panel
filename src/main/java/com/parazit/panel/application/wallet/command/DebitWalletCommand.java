package com.parazit.panel.application.wallet.command;

import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.wallet.WalletTransactionType;
import java.util.Objects;
import java.util.UUID;

public record DebitWalletCommand(
        UUID userId,
        Money amount,
        WalletTransactionType type,
        String referenceType,
        UUID referenceId,
        String idempotencyKey,
        String descriptionCode
) {

    public DebitWalletCommand {
        userId = Objects.requireNonNull(userId, "userId must not be null");
        amount = Objects.requireNonNull(amount, "amount must not be null");
        type = Objects.requireNonNull(type, "type must not be null");
        referenceType = requireText(referenceType, "referenceType");
        idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
