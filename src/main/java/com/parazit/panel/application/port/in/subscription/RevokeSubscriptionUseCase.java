package com.parazit.panel.application.port.in.subscription;

import com.parazit.panel.application.subscription.command.RevokeSubscriptionCommand;
import com.parazit.panel.application.subscription.result.SubscriptionResult;

public interface RevokeSubscriptionUseCase {

    SubscriptionResult revoke(RevokeSubscriptionCommand command);
}
