package com.parazit.panel.application.port.in.subscription.delivery;

import com.parazit.panel.application.subscription.delivery.SubscriptionDeliverySummary;
import java.util.UUID;

public interface GetSubscriptionDeliverySummaryUseCase {

    SubscriptionDeliverySummary get(Long telegramUserId, UUID subscriptionId);
}

