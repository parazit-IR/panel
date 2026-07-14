package com.parazit.panel.application.wallet;

import com.parazit.panel.application.port.in.wallet.GetOrCreateWalletUseCase;
import com.parazit.panel.application.wallet.result.WalletCreationResult;
import com.parazit.panel.config.properties.WalletProperties;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.wallet.Wallet;
import com.parazit.panel.domain.wallet.repository.WalletRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetOrCreateWalletService implements GetOrCreateWalletUseCase {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WalletProperties properties;

    public GetOrCreateWalletService(
            WalletRepository walletRepository,
            UserRepository userRepository,
            WalletProperties properties
    ) {
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    @Transactional
    public WalletCreationResult getOrCreate(UUID userId) {
        UUID requiredUserId = Objects.requireNonNull(userId, "userId must not be null");
        Wallet wallet = walletRepository.findByUserIdForUpdate(requiredUserId)
                .orElseGet(() -> createWithUserLock(requiredUserId));
        return toResult(wallet);
    }

    private Wallet createWithUserLock(UUID userId) {
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        return walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> walletRepository.save(Wallet.create(userId, properties.currency())));
    }

    private static WalletCreationResult toResult(Wallet wallet) {
        return new WalletCreationResult(wallet.getId(), wallet.balance(), wallet.getStatus());
    }
}
