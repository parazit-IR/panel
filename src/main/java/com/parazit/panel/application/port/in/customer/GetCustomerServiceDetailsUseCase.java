package com.parazit.panel.application.port.in.customer;

import com.parazit.panel.application.customer.result.CustomerServiceDetailsResult;
import java.util.UUID;

public interface GetCustomerServiceDetailsUseCase {

    CustomerServiceDetailsResult get(long telegramUserId, UUID subscriptionId, boolean refreshUsage);
}
