package com.parazit.panel.application.port.in.customer;

import com.parazit.panel.application.customer.result.CustomerServiceSummaryResult;
import java.util.List;

public interface SearchCustomerServicesUseCase {

    List<CustomerServiceSummaryResult> search(long telegramUserId, String query, int limit);
}
