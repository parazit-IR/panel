package com.parazit.panel.application.port.in.subscription.delivery;

import com.parazit.panel.application.subscription.delivery.SubscriptionRenderedContentResult;
import java.util.UUID;

public interface GetSubscriptionRenderedContentUseCase {

    SubscriptionRenderedContentResult get(Long telegramUserId, UUID subscriptionId, String format);
}

