package com.parazit.panel.application.port.in.subscription;

import com.parazit.panel.application.subscription.command.CreateSubscriptionCommand;
import com.parazit.panel.application.subscription.result.CreateSubscriptionResult;

public interface CreateSubscriptionUseCase {

    CreateSubscriptionResult create(CreateSubscriptionCommand command);
}
