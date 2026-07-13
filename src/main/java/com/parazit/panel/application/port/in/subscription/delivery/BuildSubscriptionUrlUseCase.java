package com.parazit.panel.application.port.in.subscription.delivery;

import com.parazit.panel.application.subscription.delivery.BuildSubscriptionUrlCommand;
import com.parazit.panel.application.subscription.delivery.BuildSubscriptionUrlResult;

public interface BuildSubscriptionUrlUseCase {

    BuildSubscriptionUrlResult build(BuildSubscriptionUrlCommand command);
}

