package com.parazit.panel.application.customer;

import com.parazit.panel.application.customer.result.CustomerServiceDetailsResult;
import com.parazit.panel.application.port.in.customer.GetCustomerServiceDetailsUseCase;
import com.parazit.panel.application.port.in.customer.RefreshSubscriptionUsageUseCase;
import com.parazit.panel.application.port.out.customer.CustomerServiceQueryPort;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CustomerServiceDetailsService implements GetCustomerServiceDetailsUseCase, RefreshSubscriptionUsageUseCase {

    private final CustomerServiceQueryPort queryPort;

    public CustomerServiceDetailsService(CustomerServiceQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "queryPort must not be null");
    }

    @Override
    public CustomerServiceDetailsResult get(long telegramUserId, UUID subscriptionId, boolean refreshUsage) {
        return queryPort.findDetails(telegramUserId, Objects.requireNonNull(subscriptionId, "subscriptionId must not be null"))
                .orElseThrow(() -> new IllegalArgumentException("service not found"));
    }

    @Override
    public CustomerServiceDetailsResult refresh(long telegramUserId, UUID subscriptionId, UUID refreshRequestId) {
        return get(telegramUserId, subscriptionId, true);
    }
}
