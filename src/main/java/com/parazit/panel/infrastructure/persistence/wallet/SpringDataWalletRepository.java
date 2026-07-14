package com.parazit.panel.infrastructure.persistence.wallet;

import com.parazit.panel.domain.wallet.Wallet;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;

public interface SpringDataWalletRepository extends SpringDataUuidRepository<Wallet> {

    Optional<Wallet> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Wallet> findWithLockByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
