package com.parazit.panel.infrastructure.persistence.referral;

import com.parazit.panel.domain.referral.Referral;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataReferralRepository extends SpringDataUuidRepository<Referral> {

    Optional<Referral> findByReferredUserId(UUID referredUserId);

    List<Referral> findAllByReferrerUserId(UUID referrerUserId);

    boolean existsByReferredUserId(UUID referredUserId);

    long countByReferrerUserId(UUID referrerUserId);
}
