package com.parazit.panel.application.port.out.customer;

import com.parazit.panel.application.customer.result.CustomerServiceDetailsResult;
import com.parazit.panel.application.customer.result.CustomerServiceSummaryResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerServiceQueryPort {

    List<CustomerServiceSummaryResult> findAllByTelegramUserId(long telegramUserId);

    Optional<CustomerServiceDetailsResult> findDetails(long telegramUserId, UUID subscriptionId);
}
