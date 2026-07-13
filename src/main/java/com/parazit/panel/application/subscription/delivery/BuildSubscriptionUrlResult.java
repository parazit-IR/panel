package com.parazit.panel.application.subscription.delivery;

import java.util.UUID;

public record BuildSubscriptionUrlResult(
        UUID subscriptionId,
        String subscriptionUrl
) {
}

