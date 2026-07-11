package com.parazit.panel.application.referral;

import com.parazit.panel.domain.referral.Referral;
import com.parazit.panel.domain.referral.repository.ReferralRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReferralCreationService {

    private final ReferralRepository referralRepository;

    ReferralCreationService(ReferralRepository referralRepository) {
        this.referralRepository = Objects.requireNonNull(referralRepository, "referralRepository must not be null");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Referral create(Referral referral) {
        return referralRepository.save(Objects.requireNonNull(referral, "referral must not be null"));
    }
}
