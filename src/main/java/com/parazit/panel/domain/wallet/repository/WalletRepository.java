package com.parazit.panel.domain.wallet.repository;

import com.parazit.panel.domain.repository.UuidRepository;
import com.parazit.panel.domain.wallet.Wallet;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends UuidRepository<Wallet> {

    Optional<Wallet> findByUserId(UUID userId);

    Optional<Wallet> findByUserIdForUpdate(UUID userId);

    boolean existsByUserId(UUID userId);
}
