package com.parazit.panel.application.port.in.customer;

import com.parazit.panel.application.customer.result.CustomerAccountSummaryResult;

public interface GetCustomerAccountSummaryUseCase {

    CustomerAccountSummaryResult get(long telegramUserId);
}
