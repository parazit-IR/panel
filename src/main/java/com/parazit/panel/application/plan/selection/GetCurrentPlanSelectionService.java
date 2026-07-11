package com.parazit.panel.application.plan.selection;

import com.parazit.panel.application.plan.selection.query.GetCurrentPlanSelectionQuery;
import com.parazit.panel.application.plan.selection.result.PlanSelectionResult;
import com.parazit.panel.application.port.in.plan.selection.GetCurrentPlanSelectionUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.common.exception.TraceIdFilter;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCurrentPlanSelectionService implements GetCurrentPlanSelectionUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetCurrentPlanSelectionService.class);

    private final UserRepository userRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final SystemClockPort clock;
    private final PlanSelectionResultMapper mapper;

    public GetCurrentPlanSelectionService(
            UserRepository userRepository,
            PlanSelectionRepository planSelectionRepository,
            SystemClockPort clock,
            PlanSelectionResultMapper mapper
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.planSelectionRepository = Objects.requireNonNull(planSelectionRepository, "planSelectionRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional(noRollbackFor = PlanSelectionNotFoundException.class)
    public PlanSelectionResult getCurrent(GetCurrentPlanSelectionQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        Long telegramUserId = requireTelegramUserId(query.telegramUserId());
        User user = userRepository.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new UserNotFoundException(telegramUserId));

        PlanSelection selection = planSelectionRepository.findActiveByUserId(user.getId())
                .orElseThrow(() -> new PlanSelectionNotFoundException(telegramUserId));
        Instant now = clock.now();
        if (selection.isExpiredAt(now)) {
            selection.expire(now);
            planSelectionRepository.save(selection);
            log.atDebug()
                    .addKeyValue("selectionId", selection.getId())
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("planId", selection.getPlanId())
                    .addKeyValue("planCode", selection.getPlanCodeSnapshot())
                    .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                    .log("Expired current plan selection during lookup");
            throw new PlanSelectionNotFoundException(telegramUserId);
        }

        log.atDebug()
                .addKeyValue("selectionId", selection.getId())
                .addKeyValue("userId", user.getId())
                .addKeyValue("planId", selection.getPlanId())
                .addKeyValue("planCode", selection.getPlanCodeSnapshot())
                .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                .log("Retrieved current plan selection");
        return mapper.toResult(user, selection, false);
    }

    private Long requireTelegramUserId(Long telegramUserId) {
        Objects.requireNonNull(telegramUserId, "telegramUserId must not be null");
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        return telegramUserId;
    }
}
