package com.parazit.panel.application.referral;

import com.parazit.panel.application.port.in.referral.AssignReferralUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.referral.command.AssignReferralCommand;
import com.parazit.panel.application.referral.result.AssignReferralResult;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.common.exception.TraceIdFilter;
import com.parazit.panel.domain.referral.Referral;
import com.parazit.panel.domain.referral.ReferralCodePolicy;
import com.parazit.panel.domain.referral.repository.ReferralRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignReferralService implements AssignReferralUseCase {

    private static final Logger log = LoggerFactory.getLogger(AssignReferralService.class);

    private final UserRepository userRepository;
    private final ReferralRepository referralRepository;
    private final ReferralCreationService referralCreationService;
    private final SystemClockPort systemClockPort;

    public AssignReferralService(
            UserRepository userRepository,
            ReferralRepository referralRepository,
            ReferralCreationService referralCreationService,
            SystemClockPort systemClockPort
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.referralRepository = Objects.requireNonNull(referralRepository, "referralRepository must not be null");
        this.referralCreationService = Objects.requireNonNull(referralCreationService, "referralCreationService must not be null");
        this.systemClockPort = Objects.requireNonNull(systemClockPort, "systemClockPort must not be null");
    }

    @Override
    @Transactional
    public AssignReferralResult assign(AssignReferralCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String referralCode = ReferralCodePolicy.normalizeAndValidate(command.referralCode());
        User referredUser = userRepository.findByTelegramUserId(command.referredTelegramUserId())
                .orElseThrow(() -> new UserNotFoundException(command.referredTelegramUserId()));
        User referrerUser = userRepository.findByReferralCode(referralCode)
                .orElseThrow(ReferralCodeNotFoundException::new);

        if (referrerUser.getId().equals(referredUser.getId())) {
            throw new SelfReferralNotAllowedException();
        }

        return referralRepository.findByReferredUserId(referredUser.getId())
                .map(existing -> existingResult(existing, referrerUser.getId(), referredUser.getId()))
                .orElseGet(() -> createOrRecover(referrerUser, referredUser, referralCode));
    }

    private AssignReferralResult createOrRecover(User referrerUser, User referredUser, String referralCode) {
        Instant now = systemClockPort.now();
        Referral referral = Referral.create(referrerUser.getId(), referredUser.getId(), referralCode, now);
        try {
            Referral saved = referralCreationService.create(referral);
            log.atInfo()
                    .addKeyValue("referralId", saved.getId())
                    .addKeyValue("referrerUserId", saved.getReferrerUserId())
                    .addKeyValue("referredUserId", saved.getReferredUserId())
                    .addKeyValue("newlyAssigned", true)
                    .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                    .log("Assigned referral");
            return AssignReferralResult.from(saved, true);
        } catch (DataIntegrityViolationException exception) {
            return recoverConcurrentAssignment(referrerUser.getId(), referredUser.getId());
        }
    }

    private AssignReferralResult recoverConcurrentAssignment(java.util.UUID referrerUserId, java.util.UUID referredUserId) {
        Referral existing = referralRepository.findByReferredUserId(referredUserId)
                .orElseThrow(() -> new ReferralAssignmentException("Concurrent referral assignment recovery failed"));
        return existingResult(existing, referrerUserId, referredUserId);
    }

    private AssignReferralResult existingResult(Referral existing, java.util.UUID requestedReferrerUserId, java.util.UUID referredUserId) {
        if (!existing.getReferrerUserId().equals(requestedReferrerUserId)) {
            log.atWarn()
                    .addKeyValue("referralId", existing.getId())
                    .addKeyValue("referrerUserId", requestedReferrerUserId)
                    .addKeyValue("referredUserId", referredUserId)
                    .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                    .log("Rejected conflicting referral assignment");
            throw new ReferralAlreadyAssignedException();
        }

        log.atDebug()
                .addKeyValue("referralId", existing.getId())
                .addKeyValue("referrerUserId", existing.getReferrerUserId())
                .addKeyValue("referredUserId", existing.getReferredUserId())
                .addKeyValue("newlyAssigned", false)
                .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                .log("Recovered idempotent referral assignment");
        return AssignReferralResult.from(existing, false);
    }
}
