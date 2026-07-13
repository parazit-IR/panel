package com.parazit.panel.application.port.in.customer;

import com.parazit.panel.application.customer.result.CustomerServiceDetailsResult;
import java.util.UUID;

public interface RefreshSubscriptionUsageUseCase {

    CustomerServiceDetailsResult refresh(long telegramUserId, UUID subscriptionId, UUID refreshRequestId);
}
