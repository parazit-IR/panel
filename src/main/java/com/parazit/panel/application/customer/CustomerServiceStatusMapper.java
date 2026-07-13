package com.parazit.panel.application.customer;

import com.parazit.panel.application.customer.result.CustomerServiceStatus;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.SubscriptionStatus;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class CustomerServiceStatusMapper {

    public CustomerServiceStatus map(Subscription subscription, XuiClientProvision provision, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (subscription == null) {
            return provisionStatusOnly(provision, now);
        }
        if (subscription.isExpiredAt(now) || subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            return CustomerServiceStatus.EXPIRED;
        }
        if (subscription.getStatus() == SubscriptionStatus.REVOKED || subscription.getStatus() == SubscriptionStatus.INVALID) {
            return CustomerServiceStatus.REVOKED;
        }
        if (subscription.getStatus() == SubscriptionStatus.SUSPENDED) {
            return CustomerServiceStatus.SUSPENDED;
        }
        if (subscription.getStatus() == SubscriptionStatus.PENDING) {
            return CustomerServiceStatus.PROVISIONING;
        }
        if (provision == null) {
            return subscription.getStatus() == SubscriptionStatus.ACTIVE ? CustomerServiceStatus.UNKNOWN : CustomerServiceStatus.PROVISIONING;
        }
        return switch (provision.getStatus()) {
            case PENDING, PROVISIONING, ENABLING, DISABLING, DELETING -> CustomerServiceStatus.PROVISIONING;
            case ACTIVE -> CustomerServiceStatus.ACTIVE;
            case DISABLED -> CustomerServiceStatus.SUSPENDED;
            case DELETED -> CustomerServiceStatus.REVOKED;
            case FAILED -> CustomerServiceStatus.FAILED;
            case UNKNOWN -> CustomerServiceStatus.UNKNOWN;
        };
    }

    private static CustomerServiceStatus provisionStatusOnly(XuiClientProvision provision, Instant now) {
        if (provision == null) {
            return CustomerServiceStatus.UNKNOWN;
        }
        if (!provision.getExpiresAt().isAfter(now)) {
            return CustomerServiceStatus.EXPIRED;
        }
        return switch (provision.getStatus()) {
            case PENDING, PROVISIONING, ENABLING, DISABLING, DELETING -> CustomerServiceStatus.PROVISIONING;
            case ACTIVE -> CustomerServiceStatus.ACTIVE;
            case DISABLED -> CustomerServiceStatus.SUSPENDED;
            case DELETED -> CustomerServiceStatus.REVOKED;
            case FAILED -> CustomerServiceStatus.FAILED;
            case UNKNOWN -> CustomerServiceStatus.UNKNOWN;
        };
    }
}
