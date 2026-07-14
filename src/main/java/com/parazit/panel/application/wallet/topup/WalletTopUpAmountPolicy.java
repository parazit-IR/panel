package com.parazit.panel.application.wallet.topup;

import com.parazit.panel.config.properties.WalletTopUpProperties;
import com.parazit.panel.domain.order.Money;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class WalletTopUpAmountPolicy {

    private final WalletTopUpProperties properties;

    public WalletTopUpAmountPolicy(WalletTopUpProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public Money parseCustomerInput(String input) {
        String normalized = normalizeDigits(Objects.requireNonNull(input, "input must not be null")).trim();
        if (normalized.isBlank()) {
            throw new WalletTopUpException("wallet top-up amount is invalid");
        }
        String compact = normalized
                .replace(",", "")
                .replace("٬", "")
                .replace(" ", "")
                .replace("_", "");
        if (!compact.chars().allMatch(Character::isDigit)) {
            throw new WalletTopUpException("wallet top-up amount is invalid");
        }
        try {
            return validate(new Money(Long.parseLong(compact), properties.currency()));
        } catch (NumberFormatException exception) {
            throw new WalletTopUpException("wallet top-up amount is invalid");
        }
    }

    public Money validate(Money amount) {
        Money required = Objects.requireNonNull(amount, "amount must not be null");
        if (required.currency() != properties.currency()) {
            throw new WalletTopUpException("wallet top-up currency is invalid");
        }
        if (required.amount() < properties.minimumAmount()) {
            throw new WalletTopUpException("wallet top-up amount is below minimum");
        }
        if (required.amount() > properties.maximumAmount()) {
            throw new WalletTopUpException("wallet top-up amount is above maximum");
        }
        return required;
    }

    private static String normalizeDigits(String input) {
        StringBuilder builder = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch >= '\u06F0' && ch <= '\u06F9') {
                builder.append((char) ('0' + (ch - '\u06F0')));
            } else if (ch >= '\u0660' && ch <= '\u0669') {
                builder.append((char) ('0' + (ch - '\u0660')));
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}
