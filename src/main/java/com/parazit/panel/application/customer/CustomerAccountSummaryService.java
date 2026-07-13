package com.parazit.panel.application.customer;

import com.parazit.panel.application.customer.result.CustomerAccountProjection;
import com.parazit.panel.application.customer.result.CustomerAccountStatistics;
import com.parazit.panel.application.customer.result.CustomerAccountSummaryResult;
import com.parazit.panel.application.port.in.customer.GetCustomerAccountSummaryUseCase;
import com.parazit.panel.application.port.out.customer.CustomerAccountQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CustomerAccountSummaryService implements GetCustomerAccountSummaryUseCase {

    private final CustomerAccountQueryPort queryPort;

    public CustomerAccountSummaryService(CustomerAccountQueryPort queryPort) {
        this.queryPort = java.util.Objects.requireNonNull(queryPort, "queryPort must not be null");
    }

    @Override
    public CustomerAccountSummaryResult get(long telegramUserId) {
        CustomerAccountProjection account = queryPort.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new IllegalArgumentException("customer account not found"));
        CustomerAccountStatistics statistics = queryPort.loadStatistics(account.userId());
        return new CustomerAccountSummaryResult(
                account.userId(),
                account.telegramUserId(),
                account.displayName(),
                account.username(),
                account.registeredAt(),
                account.locale(),
                account.telegramNotificationsEnabled(),
                statistics.totalServiceCount(),
                statistics.activeServiceCount(),
                statistics.expiredServiceCount(),
                statistics.paidOrderCount(),
                statistics.pendingPaymentCount(),
                Optional.empty(),
                account.phoneNumberMasked(),
                Optional.empty(),
                account.customerGroup(),
                0
        );
    }
}
