package com.parazit.panel.application.port.in.customer;

import com.parazit.panel.application.customer.result.CustomerServicePageResult;
import com.parazit.panel.application.customer.result.CustomerServiceSort;
import com.parazit.panel.application.customer.result.CustomerServiceStatusFilter;

public interface ListCustomerServicesUseCase {

    CustomerServicePageResult list(
            long telegramUserId,
            int page,
            int size,
            CustomerServiceSort sort,
            CustomerServiceStatusFilter statusFilter
    );
}
