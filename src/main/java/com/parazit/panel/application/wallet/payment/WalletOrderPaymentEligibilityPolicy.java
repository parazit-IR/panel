package com.parazit.panel.application.wallet.payment;

import com.parazit.panel.application.sales.SalesAvailabilityService;
import com.parazit.panel.config.properties.WalletPaymentProperties;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.OrderStatus;
import com.parazit.panel.domain.order.OrderType;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class WalletOrderPaymentEligibilityPolicy {

    private final SalesAvailabilityService salesAvailabilityService;
    private final WalletPaymentProperties properties;

    public WalletOrderPaymentEligibilityPolicy(
            SalesAvailabilityService salesAvailabilityService,
            WalletPaymentProperties properties
    ) {
        this.salesAvailabilityService = Objects.requireNonNull(salesAvailabilityService, "salesAvailabilityService must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public WalletOrderPaymentEligibility evaluate(UUID userId, Order order, List<Payment> payments, Instant now) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(order, "order must not be null");
        Objects.requireNonNull(payments, "payments must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (!salesAvailabilityService.walletPaymentAvailable()) {
            return WalletOrderPaymentEligibility.rejected(WalletOrderPaymentEligibilityReason.FEATURE_DISABLED);
        }
        if (!order.getUserId().equals(userId)) {
            return WalletOrderPaymentEligibility.rejected(WalletOrderPaymentEligibilityReason.ORDER_OWNER_MISMATCH);
        }
        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            if (payments.stream().anyMatch(payment -> payment.getStatus() == PaymentStatus.APPROVED)) {
                return WalletOrderPaymentEligibility.rejected(WalletOrderPaymentEligibilityReason.APPROVED_PAYMENT_EXISTS);
            }
            return WalletOrderPaymentEligibility.rejected(WalletOrderPaymentEligibilityReason.ORDER_STATUS_NOT_PAYABLE);
        }
        if (!allowedType(order.getType())) {
            return WalletOrderPaymentEligibility.rejected(WalletOrderPaymentEligibilityReason.ORDER_TYPE_NOT_ALLOWED);
        }
        if (order.getFinalAmount() <= 0) {
            return WalletOrderPaymentEligibility.rejected(WalletOrderPaymentEligibilityReason.INVALID_AMOUNT);
        }
        if (!properties.currency().name().equalsIgnoreCase(order.getCurrency())) {
            return WalletOrderPaymentEligibility.rejected(WalletOrderPaymentEligibilityReason.CURRENCY_MISMATCH);
        }
        if (order.getFinalAmount() < properties.minimumPurchaseAmount()
                || (properties.maximumPurchaseAmount() > 0 && order.getFinalAmount() > properties.maximumPurchaseAmount())) {
            return WalletOrderPaymentEligibility.rejected(WalletOrderPaymentEligibilityReason.INVALID_AMOUNT);
        }
        if (payments.stream().anyMatch(payment -> payment.getStatus() == PaymentStatus.APPROVED)) {
            return WalletOrderPaymentEligibility.rejected(WalletOrderPaymentEligibilityReason.APPROVED_PAYMENT_EXISTS);
        }
        boolean conflicting = payments.stream()
                .filter(payment -> payment.getMethod() != PaymentMethod.WALLET)
                .filter(payment -> !payment.isTerminal())
                .anyMatch(payment -> !payment.getExpiresAt().isBefore(now));
        if (conflicting) {
            return WalletOrderPaymentEligibility.rejected(WalletOrderPaymentEligibilityReason.CONFLICTING_PAYMENT_EXISTS);
        }
        return WalletOrderPaymentEligibility.allowed();
    }

    private boolean allowedType(OrderType type) {
        if (type == OrderType.NEW_SUBSCRIPTION) {
            return properties.allowNewSubscription();
        }
        if (type == OrderType.RENEWAL) {
            return properties.allowRenewal();
        }
        return false;
    }
}
