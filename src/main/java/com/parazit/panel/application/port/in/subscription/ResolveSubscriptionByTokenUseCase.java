package com.parazit.panel.application.port.in.subscription;

import com.parazit.panel.application.subscription.result.ResolvedSubscriptionContent;

public interface ResolveSubscriptionByTokenUseCase {

    ResolvedSubscriptionContent resolve(String rawToken, String requestedFormat);
}
