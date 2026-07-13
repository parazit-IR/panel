package com.parazit.panel.application.port.in.subscription;

import com.parazit.panel.application.subscription.command.RotateSubscriptionTokenCommand;
import com.parazit.panel.application.subscription.result.CreateSubscriptionResult;

public interface RotateSubscriptionTokenUseCase {

    CreateSubscriptionResult rotate(RotateSubscriptionTokenCommand command);
}
