package com.parazit.panel.infrastructure.persistence.wallet;

import com.parazit.panel.domain.wallet.WalletTransaction;
import com.parazit.panel.domain.wallet.WalletTransactionDirection;
import com.parazit.panel.domain.wallet.repository.WalletTransactionRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class WalletTransactionRepositoryAdapter
        extends JpaRepositoryAdapter<WalletTransaction, UUID>
        implements WalletTransactionRepository {

    private final SpringDataWalletTransactionRepository repository;

    public WalletTransactionRepositoryAdapter(SpringDataWalletTransactionRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public WalletTransaction save(WalletTransaction transaction) {
        return repository.saveAndFlush(Objects.requireNonNull(transaction, "transaction must not be null"));
    }

    @Override
    public Optional<WalletTransaction> findByWalletIdAndIdempotencyKey(UUID walletId, String idempotencyKey) {
        return repository.findByWalletIdAndIdempotencyKey(
                Objects.requireNonNull(walletId, "walletId must not be null"),
                requireText(idempotencyKey, "idempotencyKey")
        );
    }

    @Override
    public List<WalletTransaction> findAllByWalletIdOrderByOccurredAtDesc(UUID walletId, int offset, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        int page = Math.max(0, offset / safeLimit);
        return repository.findAllByWalletIdOrderByOccurredAtDescIdDesc(
                Objects.requireNonNull(walletId, "walletId must not be null"),
                PageRequest.of(page, safeLimit)
        );
    }

    @Override
    public long countByWalletId(UUID walletId) {
        return repository.countByWalletId(Objects.requireNonNull(walletId, "walletId must not be null"));
    }

    @Override
    public Optional<Instant> findLastOccurredAtByWalletId(UUID walletId) {
        return repository.findLastOccurredAtByWalletId(Objects.requireNonNull(walletId, "walletId must not be null"));
    }

    @Override
    public long sumCreditsByWalletId(UUID walletId) {
        return repository.sumByWalletIdAndDirection(
                Objects.requireNonNull(walletId, "walletId must not be null"),
                WalletTransactionDirection.CREDIT
        );
    }

    @Override
    public long sumDebitsByWalletId(UUID walletId) {
        return repository.sumByWalletIdAndDirection(
                Objects.requireNonNull(walletId, "walletId must not be null"),
                WalletTransactionDirection.DEBIT
        );
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
