package com.parazit.panel.application.plan.selection;

import com.parazit.panel.application.plan.selection.command.ClearPlanSelectionCommand;
import com.parazit.panel.application.plan.selection.result.PlanSelectionResult;
import com.parazit.panel.application.port.in.plan.selection.ClearPlanSelectionUseCase;
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
public class ClearPlanSelectionService implements ClearPlanSelectionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ClearPlanSelectionService.class);

    private final UserRepository userRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final SystemClockPort clock;
    private final PlanSelectionResultMapper mapper;

    public ClearPlanSelectionService(
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
    public PlanSelectionResult clear(ClearPlanSelectionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Long telegramUserId = requireTelegramUserId(command.telegramUserId());
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
                    .log("Expired current plan selection during clear");
            throw new PlanSelectionNotFoundException(telegramUserId);
        }

        selection.clear(now);
        PlanSelection saved = planSelectionRepository.save(selection);
        log.atInfo()
                .addKeyValue("selectionId", saved.getId())
                .addKeyValue("userId", user.getId())
                .addKeyValue("planId", saved.getPlanId())
                .addKeyValue("planCode", saved.getPlanCodeSnapshot())
                .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                .log("Cleared current plan selection");
        return mapper.toResult(user, saved, false);
    }

    private Long requireTelegramUserId(Long telegramUserId) {
        Objects.requireNonNull(telegramUserId, "telegramUserId must not be null");
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        return telegramUserId;
    }
}
