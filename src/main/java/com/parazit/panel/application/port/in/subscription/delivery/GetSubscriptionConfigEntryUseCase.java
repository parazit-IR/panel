package com.parazit.panel.application.port.in.subscription.delivery;

import com.parazit.panel.application.subscription.delivery.SubscriptionConfigEntryResult;
import java.util.UUID;

public interface GetSubscriptionConfigEntryUseCase {

    SubscriptionConfigEntryResult get(Long telegramUserId, UUID subscriptionId, int configIndex);
}

