package com.parazit.panel.application.wallet;

import com.parazit.panel.application.port.in.wallet.GetCustomerWalletUseCase;
import com.parazit.panel.application.port.in.wallet.GetOrCreateWalletUseCase;
import com.parazit.panel.application.wallet.command.GetCustomerWalletCommand;
import com.parazit.panel.application.wallet.result.CustomerWalletResult;
import com.parazit.panel.application.sales.SalesAvailabilityService;
import com.parazit.panel.application.wallet.result.WalletCreationResult;
import com.parazit.panel.config.properties.WalletTopUpProperties;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.wallet.repository.WalletTransactionRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCustomerWalletService implements GetCustomerWalletUseCase {

    private final UserRepository userRepository;
    private final GetOrCreateWalletUseCase getOrCreateWalletUseCase;
    private final WalletTransactionRepository transactionRepository;
    private final WalletTopUpProperties topUpProperties;
    private final SalesAvailabilityService salesAvailabilityService;

    public GetCustomerWalletService(
            UserRepository userRepository,
            GetOrCreateWalletUseCase getOrCreateWalletUseCase,
            WalletTransactionRepository transactionRepository,
            WalletTopUpProperties topUpProperties,
            SalesAvailabilityService salesAvailabilityService
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.getOrCreateWalletUseCase = Objects.requireNonNull(getOrCreateWalletUseCase, "getOrCreateWalletUseCase must not be null");
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
        this.topUpProperties = Objects.requireNonNull(topUpProperties, "topUpProperties must not be null");
        this.salesAvailabilityService = Objects.requireNonNull(salesAvailabilityService, "salesAvailabilityService must not be null");
    }

    @Override
    @Transactional
    public CustomerWalletResult get(GetCustomerWalletCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        User user = userRepository.findByTelegramUserId(command.telegramUserId())
                .orElseThrow(() -> new IllegalArgumentException("customer account not found"));
        WalletCreationResult wallet = getOrCreateWalletUseCase.getOrCreate(user.getId());
        return new CustomerWalletResult(
                wallet.walletId(),
                wallet.balance(),
                wallet.status(),
                transactionRepository.countByWalletId(wallet.walletId()),
                transactionRepository.findLastOccurredAtByWalletId(wallet.walletId()),
                topUpProperties.enabled(),
                salesAvailabilityService.walletPaymentAvailable()
        );
    }
}
