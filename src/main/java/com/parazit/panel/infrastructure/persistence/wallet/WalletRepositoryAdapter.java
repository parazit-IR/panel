package com.parazit.panel.infrastructure.persistence.wallet;

import com.parazit.panel.domain.wallet.Wallet;
import com.parazit.panel.domain.wallet.repository.WalletRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class WalletRepositoryAdapter extends JpaRepositoryAdapter<Wallet, UUID> implements WalletRepository {

    private final SpringDataWalletRepository repository;

    public WalletRepositoryAdapter(SpringDataWalletRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Wallet save(Wallet wallet) {
        return repository.saveAndFlush(Objects.requireNonNull(wallet, "wallet must not be null"));
    }

    @Override
    public Optional<Wallet> findByUserId(UUID userId) {
        return repository.findByUserId(Objects.requireNonNull(userId, "userId must not be null"));
    }

    @Override
    public Optional<Wallet> findByUserIdForUpdate(UUID userId) {
        return repository.findWithLockByUserId(Objects.requireNonNull(userId, "userId must not be null"));
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return repository.existsByUserId(Objects.requireNonNull(userId, "userId must not be null"));
    }
}
