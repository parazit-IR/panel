package com.parazit.panel.infrastructure.persistence.referral;

import com.parazit.panel.domain.referral.Referral;
import com.parazit.panel.domain.referral.repository.ReferralRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ReferralRepositoryAdapter extends JpaRepositoryAdapter<Referral, UUID> implements ReferralRepository {

    private final SpringDataReferralRepository repository;

    public ReferralRepositoryAdapter(SpringDataReferralRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Referral save(Referral referral) {
        return repository.saveAndFlush(Objects.requireNonNull(referral, "referral must not be null"));
    }

    @Override
    public Optional<Referral> findByReferredUserId(UUID referredUserId) {
        return repository.findByReferredUserId(requireUserId(referredUserId, "referredUserId"));
    }

    @Override
    public List<Referral> findAllByReferrerUserId(UUID referrerUserId) {
        return repository.findAllByReferrerUserId(requireUserId(referrerUserId, "referrerUserId"));
    }

    @Override
    public boolean existsByReferredUserId(UUID referredUserId) {
        return repository.existsByReferredUserId(requireUserId(referredUserId, "referredUserId"));
    }

    @Override
    public long countByReferrerUserId(UUID referrerUserId) {
        return repository.countByReferrerUserId(requireUserId(referrerUserId, "referrerUserId"));
    }

    private UUID requireUserId(UUID userId, String fieldName) {
        return Objects.requireNonNull(userId, fieldName + " must not be null");
    }
}
