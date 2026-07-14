package com.parazit.panel.infrastructure.persistence.wallet.topup;

import com.parazit.panel.domain.wallet.topup.WalletTopUpRequest;
import com.parazit.panel.domain.wallet.topup.WalletTopUpStatus;
import com.parazit.panel.domain.wallet.topup.repository.WalletTopUpRequestRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class WalletTopUpRequestRepositoryAdapter
        extends JpaRepositoryAdapter<WalletTopUpRequest, UUID>
        implements WalletTopUpRequestRepository {

    private final SpringDataWalletTopUpRequestRepository repository;

    public WalletTopUpRequestRepositoryAdapter(SpringDataWalletTopUpRequestRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public WalletTopUpRequest save(WalletTopUpRequest request) {
        return repository.saveAndFlush(Objects.requireNonNull(request, "request must not be null"));
    }

    @Override
    public Optional<WalletTopUpRequest> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(Objects.requireNonNull(id, "id must not be null"));
    }

    @Override
    public Optional<WalletTopUpRequest> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey) {
        return repository.findByUserIdAndIdempotencyKey(
                Objects.requireNonNull(userId, "userId must not be null"),
                requireText(idempotencyKey, "idempotencyKey")
        );
    }

    @Override
    public Optional<WalletTopUpRequest> findByPaymentId(UUID paymentId) {
        return repository.findByPaymentId(Objects.requireNonNull(paymentId, "paymentId must not be null"));
    }

    @Override
    public long countByUserIdAndStatusIn(UUID userId, Collection<WalletTopUpStatus> statuses) {
        Objects.requireNonNull(statuses, "statuses must not be null");
        return repository.countByUserIdAndStatusIn(Objects.requireNonNull(userId, "userId must not be null"), statuses);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
