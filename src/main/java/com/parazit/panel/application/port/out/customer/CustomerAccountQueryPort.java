package com.parazit.panel.application.port.out.customer;

import com.parazit.panel.application.customer.result.CustomerAccountProjection;
import com.parazit.panel.application.customer.result.CustomerAccountStatistics;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAccountQueryPort {

    Optional<CustomerAccountProjection> findByTelegramUserId(long telegramUserId);

    CustomerAccountStatistics loadStatistics(UUID userId);
}
