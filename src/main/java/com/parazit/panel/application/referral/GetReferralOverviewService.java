package com.parazit.panel.application.referral;

import com.parazit.panel.application.port.in.referral.GetReferralOverviewUseCase;
import com.parazit.panel.application.referral.query.GetReferralOverviewQuery;
import com.parazit.panel.application.referral.result.ReferralOverviewResult;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.domain.referral.Referral;
import com.parazit.panel.domain.referral.repository.ReferralRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetReferralOverviewService implements GetReferralOverviewUseCase {

    private final UserRepository userRepository;
    private final ReferralRepository referralRepository;
    private final EnsureUserReferralCodeService ensureUserReferralCodeService;

    public GetReferralOverviewService(
            UserRepository userRepository,
            ReferralRepository referralRepository,
            EnsureUserReferralCodeService ensureUserReferralCodeService
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.referralRepository = Objects.requireNonNull(referralRepository, "referralRepository must not be null");
        this.ensureUserReferralCodeService = Objects.requireNonNull(ensureUserReferralCodeService, "ensureUserReferralCodeService must not be null");
    }

    @Override
    @Transactional
    public ReferralOverviewResult getOverview(GetReferralOverviewQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        User user = userRepository.findByTelegramUserId(query.telegramUserId())
                .orElseThrow(() -> new UserNotFoundException(query.telegramUserId()));
        String referralCode = ensureUserReferralCodeService.ensureReferralCode(user);
        long referralCount = referralRepository.countByReferrerUserId(user.getId());

        Referral existingReferral = referralRepository.findByReferredUserId(user.getId()).orElse(null);
        User referrer = existingReferral == null
                ? null
                : userRepository.findById(existingReferral.getReferrerUserId()).orElse(null);

        return new ReferralOverviewResult(
                user.getId(),
                user.getTelegramUserId(),
                referralCode,
                referralCount,
                existingReferral == null ? null : existingReferral.getReferrerUserId(),
                referrer == null ? null : referrer.getTelegramUserId()
        );
    }
}
