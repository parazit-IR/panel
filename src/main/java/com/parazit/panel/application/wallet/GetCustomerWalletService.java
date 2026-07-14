package com.parazit.panel.application.wallet;

import com.parazit.panel.application.port.in.wallet.GetCustomerWalletUseCase;
import com.parazit.panel.application.port.in.wallet.GetOrCreateWalletUseCase;
import com.parazit.panel.application.wallet.command.GetCustomerWalletCommand;
import com.parazit.panel.application.wallet.result.CustomerWalletResult;
import com.parazit.panel.application.wallet.result.WalletCreationResult;
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

    public GetCustomerWalletService(
            UserRepository userRepository,
            GetOrCreateWalletUseCase getOrCreateWalletUseCase,
            WalletTransactionRepository transactionRepository
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.getOrCreateWalletUseCase = Objects.requireNonNull(getOrCreateWalletUseCase, "getOrCreateWalletUseCase must not be null");
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
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
                false,
                false
        );
    }
}
