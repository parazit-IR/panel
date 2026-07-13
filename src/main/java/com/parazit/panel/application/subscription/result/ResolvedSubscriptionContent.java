package com.parazit.panel.application.subscription.result;

import com.parazit.panel.application.subscription.model.RenderedSubscription;
import java.util.UUID;

public record ResolvedSubscriptionContent(
        UUID subscriptionId,
        UUID provisionId,
        String format,
        RenderedSubscription rendered
) {
}
