package com.parazit.panel.application.promotion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.config.properties.PromotionProperties;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.promotion.DiscountCode;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DiscountAmountCalculatorTest {

    private final DiscountAmountCalculator calculator = new DiscountAmountCalculator(new PromotionProperties(
            true,
            true,
            4,
            32,
            Duration.ofMinutes(30),
            true,
            true,
            false,
            false,
            "secret"
    ));

    @Test
    void calculatesFixedDiscount() {
        DiscountCode code = DiscountCode.fixed(
                "HASH",
                "HA***SH",
                new Money(20_000, CurrencyCode.IRT),
                new Money(0, CurrencyCode.IRT),
                null,
                true,
                true,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2027-01-01T00:00:00Z"),
                10,
                1
        );

        DiscountCalculationResult result = calculator.calculate(code, new Money(100_000, CurrencyCode.IRT));

        assertThat(result.discountAmount().amount()).isEqualTo(20_000);
        assertThat(result.finalAmount().amount()).isEqualTo(80_000);
    }

    @Test
    void calculatesPercentageWithCap() {
        DiscountCode code = DiscountCode.percentage(
                "HASH2",
                "HA***H2",
                2_500,
                CurrencyCode.IRT,
                new Money(0, CurrencyCode.IRT),
                new Money(15_000, CurrencyCode.IRT),
                true,
                true,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2027-01-01T00:00:00Z"),
                10,
                1
        );

        DiscountCalculationResult result = calculator.calculate(code, new Money(100_000, CurrencyCode.IRT));

        assertThat(result.discountAmount().amount()).isEqualTo(15_000);
        assertThat(result.finalAmount().amount()).isEqualTo(85_000);
    }

    @Test
    void rejectsZeroFinalAmountWhenDisabled() {
        DiscountCode code = DiscountCode.fixed(
                "HASH3",
                "HA***H3",
                new Money(100_000, CurrencyCode.IRT),
                new Money(0, CurrencyCode.IRT),
                null,
                true,
                true,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2027-01-01T00:00:00Z"),
                10,
                1
        );

        assertThatThrownBy(() -> calculator.calculate(code, new Money(100_000, CurrencyCode.IRT)))
                .isInstanceOf(PromotionException.class);
    }
}
