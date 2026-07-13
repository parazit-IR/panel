package com.parazit.panel.application.port.in.subscription;

import com.parazit.panel.application.subscription.result.SubscriptionResult;
import java.util.UUID;

public interface SuspendSubscriptionUseCase {

    SubscriptionResult suspend(UUID subscriptionId);
}
