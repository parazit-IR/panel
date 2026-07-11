package com.parazit.panel.application.plan.selection;

import com.parazit.panel.application.plan.catalog.AvailablePlanNotFoundException;
import com.parazit.panel.application.plan.selection.command.SelectPlanCommand;
import com.parazit.panel.application.plan.selection.result.PlanSelectionResult;
import com.parazit.panel.application.port.in.plan.selection.SelectPlanUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.common.exception.TraceIdFilter;
import com.parazit.panel.config.properties.PlanSelectionProperties;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SelectPlanService implements SelectPlanUseCase {

    private static final Logger log = LoggerFactory.getLogger(SelectPlanService.class);

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final SystemClockPort clock;
    private final PlanSelectionProperties properties;
    private final PlanSelectionEligibilityPolicy eligibilityPolicy;
    private final PlanSelectionResultMapper mapper;

    public SelectPlanService(
            UserRepository userRepository,
            PlanRepository planRepository,
            PlanSelectionRepository planSelectionRepository,
            SystemClockPort clock,
            PlanSelectionProperties properties,
            PlanSelectionEligibilityPolicy eligibilityPolicy,
            PlanSelectionResultMapper mapper
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.planRepository = Objects.requireNonNull(planRepository, "planRepository must not be null");
        this.planSelectionRepository = Objects.requireNonNull(planSelectionRepository, "planSelectionRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.eligibilityPolicy = Objects.requireNonNull(eligibilityPolicy, "eligibilityPolicy must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public PlanSelectionResult select(SelectPlanCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Long telegramUserId = requireTelegramUserId(command.telegramUserId());
        UUID planId = Objects.requireNonNull(command.planId(), "planId must not be null");
        Instant now = clock.now();

        User user = userRepository.findByTelegramUserIdForUpdate(telegramUserId)
                .orElseThrow(() -> new UserNotFoundException(telegramUserId));
        eligibilityPolicy.verifyEligible(user);
        Plan plan = planRepository.findByIdAndStatus(planId, PlanStatus.ACTIVE)
                .orElseThrow(() -> new AvailablePlanNotFoundException(planId));

        PlanSelection current = planSelectionRepository.findActiveByUserId(user.getId()).orElse(null);
        if (current != null && current.isExpiredAt(now)) {
            current.expire(now);
            planSelectionRepository.save(current);
            logExpiration(current, user);
            current = null;
        }

        if (current != null && current.getPlanId().equals(plan.getId())) {
            log.atDebug()
                    .addKeyValue("selectionId", current.getId())
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("planId", plan.getId())
                    .addKeyValue("planCode", plan.getCode())
                    .addKeyValue("newlySelected", false)
                    .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                    .log("Plan selection is already current");
            return mapper.toResult(user, current, false);
        }

        if (current != null) {
            current.clear(now);
            planSelectionRepository.save(current);
            log.atInfo()
                    .addKeyValue("oldSelectionId", current.getId())
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("planId", plan.getId())
                    .addKeyValue("planCode", plan.getCode())
                    .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                    .log("Replaced current plan selection");
        }

        PlanSelection selection = PlanSelection.create(user.getId(), plan, now, properties.ttl());
        try {
            PlanSelection saved = planSelectionRepository.save(selection);
            log.atInfo()
                    .addKeyValue("selectionId", saved.getId())
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("planId", saved.getPlanId())
                    .addKeyValue("planCode", saved.getPlanCodeSnapshot())
                    .addKeyValue("newlySelected", true)
                    .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                    .log("Selected plan");
            return mapper.toResult(user, saved, true);
        } catch (DataIntegrityViolationException exception) {
            log.atWarn()
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("planId", planId)
                    .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                    .log("Concurrent plan selection conflict");
            throw new PlanSelectionConflictException(telegramUserId);
        }
    }

    private void logExpiration(PlanSelection selection, User user) {
        log.atDebug()
                .addKeyValue("selectionId", selection.getId())
                .addKeyValue("userId", user.getId())
                .addKeyValue("planId", selection.getPlanId())
                .addKeyValue("planCode", selection.getPlanCodeSnapshot())
                .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                .log("Expired current plan selection");
    }

    private Long requireTelegramUserId(Long telegramUserId) {
        Objects.requireNonNull(telegramUserId, "telegramUserId must not be null");
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        return telegramUserId;
    }
}
