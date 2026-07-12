package com.parazit.panel.infrastructure.persistence.payment.zarinpal;

import com.parazit.panel.domain.payment.zarinpal.ZarinpalAttemptStatus;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalPaymentAttempt;
import com.parazit.panel.domain.payment.zarinpal.repository.ZarinpalPaymentAttemptRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ZarinpalPaymentAttemptRepositoryAdapter extends JpaRepositoryAdapter<ZarinpalPaymentAttempt, UUID>
        implements ZarinpalPaymentAttemptRepository {

    private final SpringDataZarinpalPaymentAttemptRepository repository;

    public ZarinpalPaymentAttemptRepositoryAdapter(SpringDataZarinpalPaymentAttemptRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public ZarinpalPaymentAttempt save(ZarinpalPaymentAttempt attempt) {
        return repository.saveAndFlush(Objects.requireNonNull(attempt, "attempt must not be null"));
    }

    @Override
    public Optional<ZarinpalPaymentAttempt> findByRequestId(UUID requestId) {
        return repository.findByRequestId(Objects.requireNonNull(requestId, "requestId must not be null"));
    }

    @Override
    public Optional<ZarinpalPaymentAttempt> findByAuthority(String authority) {
        return repository.findByAuthority(ZarinpalPaymentAttempt.normalizeAuthority(authority));
    }

    @Override
    public Optional<ZarinpalPaymentAttempt> findVerifiedByPaymentId(UUID paymentId) {
        return repository.findFirstByPaymentIdAndStatus(
                Objects.requireNonNull(paymentId, "paymentId must not be null"),
                ZarinpalAttemptStatus.VERIFIED
        );
    }

    @Override
    public List<ZarinpalPaymentAttempt> findAllByPaymentIdOrderByCreatedAtDesc(UUID paymentId) {
        return repository.findAllByPaymentIdOrderByCreatedAtDesc(
                Objects.requireNonNull(paymentId, "paymentId must not be null")
        );
    }

    @Override
    public boolean existsByAuthority(String authority) {
        return repository.existsByAuthority(ZarinpalPaymentAttempt.normalizeAuthority(authority));
    }
}
