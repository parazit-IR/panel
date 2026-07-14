package com.parazit.panel.application.customer;

import com.parazit.panel.application.customer.result.CustomerAccountProjection;
import com.parazit.panel.application.customer.result.CustomerAccountStatistics;
import com.parazit.panel.application.customer.result.CustomerAccountSummaryResult;
import com.parazit.panel.application.port.in.customer.GetCustomerAccountSummaryUseCase;
import com.parazit.panel.application.port.out.customer.CustomerAccountQueryPort;
import com.parazit.panel.config.properties.WalletProperties;
import com.parazit.panel.domain.wallet.Wallet;
import com.parazit.panel.domain.wallet.repository.WalletRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CustomerAccountSummaryService implements GetCustomerAccountSummaryUseCase {

    private final CustomerAccountQueryPort queryPort;
    private final WalletRepository walletRepository;
    private final WalletProperties walletProperties;

    public CustomerAccountSummaryService(
            CustomerAccountQueryPort queryPort,
            WalletRepository walletRepository,
            WalletProperties walletProperties
    ) {
        this.queryPort = java.util.Objects.requireNonNull(queryPort, "queryPort must not be null");
        this.walletRepository = java.util.Objects.requireNonNull(walletRepository, "walletRepository must not be null");
        this.walletProperties = java.util.Objects.requireNonNull(walletProperties, "walletProperties must not be null");
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
                walletBalance(account),
                account.customerGroup(),
                0
        );
    }

    private Optional<com.parazit.panel.domain.order.Money> walletBalance(CustomerAccountProjection account) {
        if (!walletProperties.enabled()) {
            return Optional.empty();
        }
        return walletRepository.findByUserId(account.userId()).map(Wallet::balance);
    }
}
