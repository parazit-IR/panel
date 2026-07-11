package com.parazit.panel.domain.referral.repository;

import com.parazit.panel.domain.referral.Referral;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferralRepository extends UuidRepository<Referral> {

    Optional<Referral> findByReferredUserId(UUID referredUserId);

    List<Referral> findAllByReferrerUserId(UUID referrerUserId);

    boolean existsByReferredUserId(UUID referredUserId);

    long countByReferrerUserId(UUID referrerUserId);
}
