package com.parazit.panel.application.promotion;

import com.parazit.panel.application.sales.SalesAvailabilityService;
import com.parazit.panel.config.properties.PromotionProperties;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.OrderStatus;
import com.parazit.panel.domain.order.OrderType;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.promotion.DiscountCode;
import com.parazit.panel.domain.promotion.DiscountCodeStatus;
import com.parazit.panel.domain.promotion.DiscountRejectionReason;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DiscountEligibilityPolicy {

    private final SalesAvailabilityService salesAvailabilityService;
    private final PromotionProperties properties;

    public DiscountEligibilityPolicy(SalesAvailabilityService salesAvailabilityService, PromotionProperties properties) {
        this.salesAvailabilityService = Objects.requireNonNull(salesAvailabilityService, "salesAvailabilityService must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public DiscountRejectionReason evaluate(UUID userId, Order order, DiscountCode code, List<Payment> payments, Instant now) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(order, "order must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(payments, "payments must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (!salesAvailabilityService.availability(com.parazit.panel.application.sales.SalesCapability.DISCOUNT_CODE).enabled()
                || !properties.discountEnabled()) {
            return DiscountRejectionReason.FEATURE_DISABLED;
        }
        if (!order.getUserId().equals(userId)
                || (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PAYMENT_PENDING)) {
            return DiscountRejectionReason.ORDER_NOT_ELIGIBLE;
        }
        if (!payments.isEmpty()) {
            return DiscountRejectionReason.PAYMENT_EXISTS;
        }
        if (order.getAppliedDiscountCodeId() != null && !code.getId().equals(order.getAppliedDiscountCodeId())) {
            return DiscountRejectionReason.STACKING_NOT_ALLOWED;
        }
        if (!code.isActive() || code.getStatus() != DiscountCodeStatus.ACTIVE) {
            return DiscountRejectionReason.NOT_ACTIVE;
        }
        if (now.isBefore(code.getValidFrom())) {
            return DiscountRejectionReason.NOT_STARTED;
        }
        if (!now.isBefore(code.getValidUntil())) {
            return DiscountRejectionReason.EXPIRED;
        }
        if (code.getTotalUsageLimit() > 0 && code.getUsedCount() >= code.getTotalUsageLimit()) {
            return DiscountRejectionReason.EXHAUSTED;
        }
        if (!code.eligibleFor(order.getType())) {
            return DiscountRejectionReason.WRONG_ORDER_TYPE;
        }
        if (order.getType() == OrderType.NEW_SUBSCRIPTION && !properties.allowDiscountOnNewSubscription()) {
            return DiscountRejectionReason.WRONG_ORDER_TYPE;
        }
        if (order.getType() == OrderType.RENEWAL && !properties.allowDiscountOnRenewal()) {
            return DiscountRejectionReason.WRONG_ORDER_TYPE;
        }
        if (!code.getCurrency().equalsIgnoreCase(order.getCurrency())) {
            return DiscountRejectionReason.CURRENCY_MISMATCH;
        }
        if (order.getBaseAmount() < code.getMinimumOrderAmount()) {
            return DiscountRejectionReason.MINIMUM_AMOUNT_NOT_MET;
        }
        return DiscountRejectionReason.NONE;
    }

    public String messageKey(DiscountRejectionReason reason) {
        return switch (reason) {
            case MINIMUM_AMOUNT_NOT_MET -> "telegram.promotion.minimum_not_met";
            case PLAN_NOT_ELIGIBLE, WRONG_ORDER_TYPE -> "telegram.promotion.not_eligible_for_plan";
            case USER_LIMIT_REACHED -> "telegram.promotion.user_limit";
            default -> "telegram.promotion.invalid_code";
        };
    }
}
