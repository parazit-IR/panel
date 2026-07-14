package com.parazit.panel.domain.promotion;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.plan.CurrencyCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PromotionCodeTest {

    @Test
    void discountToStringDoesNotExposeHash() {
        DiscountCode code = DiscountCode.fixed(
                "SECRET_HASH",
                "SE***SH",
                new Money(1_000, CurrencyCode.IRT),
                new Money(0, CurrencyCode.IRT),
                null,
                true,
                true,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2027-01-01T00:00:00Z"),
                1,
                1
        );

        assertThat(code.toString()).contains("SE***SH").doesNotContain("SECRET_HASH");
    }

    @Test
    void giftToStringDoesNotExposeHash() {
        GiftCode code = GiftCode.create(
                "SECRET_HASH",
                "SE***SH",
                new Money(1_000, CurrencyCode.IRT),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2027-01-01T00:00:00Z"),
                1,
                1
        );

        assertThat(code.toString()).contains("SE***SH").doesNotContain("SECRET_HASH");
    }
}
