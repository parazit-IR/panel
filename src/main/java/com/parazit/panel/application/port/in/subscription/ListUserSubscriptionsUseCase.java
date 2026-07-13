package com.parazit.panel.application.port.in.subscription;

import com.parazit.panel.application.subscription.result.SubscriptionResult;
import java.util.List;

public interface ListUserSubscriptionsUseCase {

    List<SubscriptionResult> list(Long telegramUserId);
}
