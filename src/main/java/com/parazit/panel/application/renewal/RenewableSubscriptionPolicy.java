package com.parazit.panel.application.renewal;

import com.parazit.panel.config.properties.RenewalProperties;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.SubscriptionStatus;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RenewableSubscriptionPolicy {

    private final RenewalProperties properties;

    public RenewableSubscriptionPolicy(RenewalProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public RenewableSubscriptionDecision evaluate(
            UUID userId,
            Subscription subscription,
            XuiClientProvision provision,
            boolean hasActiveRenewalOrder,
            Instant now
    ) {
        Objects.requireNonNull(now, "now must not be null");
        if (!properties.enabled()) {
            return reject(RenewalIneligibilityReason.RENEWAL_DISABLED);
        }
        if (subscription == null) {
            return reject(RenewalIneligibilityReason.SUBSCRIPTION_NOT_FOUND);
        }
        if (userId != null && !userId.equals(subscription.getUserId())) {
            return reject(RenewalIneligibilityReason.OWNERSHIP_MISMATCH);
        }
        if (hasActiveRenewalOrder) {
            return reject(RenewalIneligibilityReason.ALREADY_HAS_ACTIVE_RENEWAL_ORDER);
        }
        if (subscription.getStatus() == SubscriptionStatus.REVOKED || subscription.getStatus() == SubscriptionStatus.INVALID) {
            return reject(RenewalIneligibilityReason.SUBSCRIPTION_REVOKED);
        }
        if (subscription.getStatus() == SubscriptionStatus.SUSPENDED && !properties.allowSuspendedSubscription()) {
            return reject(RenewalIneligibilityReason.SUBSCRIPTION_SUSPENDED);
        }
        if (subscription.getStatus() == SubscriptionStatus.PENDING) {
            return reject(RenewalIneligibilityReason.PROVISIONING_IN_PROGRESS);
        }
        boolean expired = subscription.getStatus() == SubscriptionStatus.EXPIRED || subscription.isExpiredAt(now);
        if (!expired && subscription.getStatus() == SubscriptionStatus.ACTIVE && !properties.allowActiveSubscription()) {
            return reject(RenewalIneligibilityReason.UNKNOWN_STATE);
        }
        if (expired && !properties.allowExpiredSubscription()) {
            return reject(RenewalIneligibilityReason.INVALID_EXPIRY);
        }
        if (provision == null) {
            return reject(RenewalIneligibilityReason.PROVISION_NOT_FOUND);
        }
        if (!subscription.getXuiClientProvisionId().equals(provision.getId())) {
            return reject(RenewalIneligibilityReason.PROVISION_NOT_FOUND);
        }
        if (properties.requireSuccessfulProvision() && provision.getStatus() != XuiProvisionStatus.ACTIVE) {
            return switch (provision.getStatus()) {
                case PENDING, PROVISIONING, ENABLING, DISABLING, DELETING -> reject(RenewalIneligibilityReason.PROVISIONING_IN_PROGRESS);
                case FAILED, UNKNOWN -> reject(RenewalIneligibilityReason.PROVISIONING_FAILED);
                case DISABLED -> reject(RenewalIneligibilityReason.SUBSCRIPTION_SUSPENDED);
                case DELETED -> reject(RenewalIneligibilityReason.SUBSCRIPTION_REVOKED);
                case ACTIVE -> RenewableSubscriptionDecision.accepted();
            };
        }
        if (properties.requireRemoteClientReference() && isBlank(provision.getRemoteClientId())) {
            return reject(RenewalIneligibilityReason.MISSING_REMOTE_CLIENT_REFERENCE);
        }
        if (provision.getTrafficLimitBytes() < 0 || provision.getLastKnownTotalBytes() < 0) {
            return reject(RenewalIneligibilityReason.INVALID_TRAFFIC_STATE);
        }
        return RenewableSubscriptionDecision.accepted();
    }

    private static RenewableSubscriptionDecision reject(RenewalIneligibilityReason reason) {
        return RenewableSubscriptionDecision.rejected(reason, messageKey(reason));
    }

    public static String messageKey(RenewalIneligibilityReason reason) {
        return switch (reason) {
            case RENEWAL_DISABLED -> "telegram.renewal.not_available";
            case ALREADY_HAS_ACTIVE_RENEWAL_ORDER -> "telegram.renewal.existing_order";
            case SUBSCRIPTION_NOT_FOUND, OWNERSHIP_MISMATCH -> "telegram.renewal.not_available";
            default -> "telegram.renewal.cannot_renew";
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
