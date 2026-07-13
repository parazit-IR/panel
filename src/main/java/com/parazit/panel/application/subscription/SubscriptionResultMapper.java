package com.parazit.panel.application.subscription;

import com.parazit.panel.application.subscription.result.CreateSubscriptionResult;
import com.parazit.panel.application.subscription.result.SubscriptionResult;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.subscription.Subscription;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionResultMapper {

    public CreateSubscriptionResult toCreateResult(
            Subscription subscription,
            String rawToken,
            boolean newlyCreated
    ) {
        return new CreateSubscriptionResult(
                subscription.getId(),
                subscription.getUserId(),
                subscription.getOrderId(),
                subscription.getXuiClientProvisionId(),
                subscription.getStatus(),
                rawToken,
                subscription.getAccessTokenPrefix(),
                subscription.getTokenVersion(),
                subscription.getActivatedAt(),
                subscription.getExpiresAt(),
                newlyCreated
        );
    }

    public SubscriptionResult toResult(Subscription subscription, PlanSelection selection, Instant now) {
        return new SubscriptionResult(
                subscription.getId(),
                subscription.getOrderId(),
                subscription.getXuiClientProvisionId(),
                selection.getPlanNameSnapshot(),
                subscription.getStatus(),
                subscription.getAccessTokenPrefix(),
                subscription.getTokenVersion(),
                subscription.getActivatedAt(),
                subscription.getExpiresAt(),
                subscription.getRevokedAt(),
                subscription.getLastAccessedAt(),
                subscription.getAccessCount(),
                subscription.isAccessibleAt(now)
        );
    }
}
