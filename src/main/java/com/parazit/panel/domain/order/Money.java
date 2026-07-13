package com.parazit.panel.domain.order;

import com.parazit.panel.domain.plan.CurrencyCode;
import java.util.Objects;

public record Money(long amount, CurrencyCode currency) {

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be zero or positive");
        }
        currency = Objects.requireNonNull(currency, "currency must not be null");
    }
}
