package com.parazit.panel.application.promotion;

import com.parazit.panel.config.properties.PromotionProperties;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.promotion.DiscountCode;
import com.parazit.panel.domain.promotion.DiscountType;
import java.math.BigInteger;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class DiscountAmountCalculator {

    private final PromotionProperties properties;

    public DiscountAmountCalculator(PromotionProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public DiscountCalculationResult calculate(DiscountCode code, Money originalAmount) {
        DiscountCode requiredCode = Objects.requireNonNull(code, "code must not be null");
        Money original = Objects.requireNonNull(originalAmount, "originalAmount must not be null");
        if (requiredCode.currencyCode() != original.currency()) {
            throw new PromotionException("telegram.promotion.invalid_code");
        }
        long discount = requiredCode.getDiscountType() == DiscountType.FIXED_AMOUNT
                ? requiredCode.fixedMoney().amount()
                : percentage(original.amount(), requiredCode.getPercentageBasisPoints());
        if (requiredCode.maximumDiscountMoney() != null) {
            discount = Math.min(discount, requiredCode.maximumDiscountMoney().amount());
        }
        discount = Math.min(discount, original.amount());
        long finalAmount = Math.max(original.amount() - discount, 0L);
        if (!properties.allowZeroFinalAmount() && finalAmount == 0L) {
            throw new PromotionException("telegram.promotion.zero_final_not_allowed");
        }
        return new DiscountCalculationResult(
                original,
                new Money(discount, original.currency()),
                new Money(finalAmount, original.currency())
        );
    }

    private static long percentage(long amount, int basisPoints) {
        return BigInteger.valueOf(amount)
                .multiply(BigInteger.valueOf(basisPoints))
                .divide(BigInteger.valueOf(10_000L))
                .longValueExact();
    }
}
