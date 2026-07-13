package com.parazit.panel.application.renewal;

import java.util.Optional;

public record RenewableSubscriptionDecision(
        boolean renewable,
        RenewalIneligibilityReason reason,
        String messageKey
) {

    public static RenewableSubscriptionDecision accepted() {
        return new RenewableSubscriptionDecision(true, null, "");
    }

    public static RenewableSubscriptionDecision rejected(RenewalIneligibilityReason reason, String messageKey) {
        return new RenewableSubscriptionDecision(false, reason, messageKey);
    }

    public Optional<RenewalIneligibilityReason> optionalReason() {
        return Optional.ofNullable(reason);
    }
}
