package com.parazit.panel.application.referral;

import com.parazit.panel.application.port.out.referral.ReferralCodeGenerator;
import com.parazit.panel.common.exception.TraceIdFilter;
import com.parazit.panel.domain.referral.ReferralCodePolicy;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnsureUserReferralCodeService {

    private static final Logger log = LoggerFactory.getLogger(EnsureUserReferralCodeService.class);
    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final ReferralCodeGenerator referralCodeGenerator;

    public EnsureUserReferralCodeService(
            UserRepository userRepository,
            ReferralCodeGenerator referralCodeGenerator
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.referralCodeGenerator = Objects.requireNonNull(referralCodeGenerator, "referralCodeGenerator must not be null");
    }

    @Transactional
    public String ensureReferralCode(User user) {
        Objects.requireNonNull(user, "user must not be null");
        if (user.getReferralCode() != null) {
            return user.getReferralCode();
        }

        assignReferralCode(user);
        try {
            User saved = userRepository.save(user);
            return saved.getReferralCode();
        } catch (DataIntegrityViolationException exception) {
            throw new ReferralAssignmentException("Could not assign referral code", exception);
        }
    }

    public String assignReferralCode(User user) {
        Objects.requireNonNull(user, "user must not be null");
        if (user.getReferralCode() != null) {
            return user.getReferralCode();
        }

        String code = generateAvailableReferralCode();
        user.assignReferralCode(code);
        return code;
    }

    private String generateAvailableReferralCode() {
        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            String code = ReferralCodePolicy.normalizeAndValidate(referralCodeGenerator.generate());
            if (!userRepository.existsByReferralCode(code)) {
                return code;
            }
            log.atDebug()
                    .addKeyValue("attempt", attempt)
                    .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                    .log("Generated duplicate referral code; retrying");
        }
        throw new ReferralAssignmentException("Could not generate a unique referral code");
    }
}
