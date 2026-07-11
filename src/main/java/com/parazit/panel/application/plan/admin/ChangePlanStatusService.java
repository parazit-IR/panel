package com.parazit.panel.application.plan.admin;

import com.parazit.panel.application.plan.PlanResultMapper;
import com.parazit.panel.application.plan.admin.command.ChangePlanStatusCommand;
import com.parazit.panel.application.plan.result.PlanResult;
import com.parazit.panel.application.port.in.plan.admin.ChangePlanStatusUseCase;
import com.parazit.panel.common.exception.TraceIdFilter;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangePlanStatusService implements ChangePlanStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChangePlanStatusService.class);

    private final PlanRepository planRepository;
    private final PlanResultMapper mapper;

    public ChangePlanStatusService(PlanRepository planRepository, PlanResultMapper mapper) {
        this.planRepository = Objects.requireNonNull(planRepository, "planRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public PlanResult activate(ChangePlanStatusCommand command) {
        return changeStatus(command, "activate", Plan::activate);
    }

    @Override
    @Transactional
    public PlanResult deactivate(ChangePlanStatusCommand command) {
        return changeStatus(command, "deactivate", Plan::deactivate);
    }

    @Override
    @Transactional
    public PlanResult archive(ChangePlanStatusCommand command) {
        return changeStatus(command, "archive", Plan::archive);
    }

    private PlanResult changeStatus(ChangePlanStatusCommand command, String action, PlanStatusMutation mutation) {
        Objects.requireNonNull(command, "command must not be null");
        UUID planId = Objects.requireNonNull(command.planId(), "planId must not be null");
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException(planId));
        PlanStatus oldStatus = plan.getStatus();

        try {
            mutation.apply(plan);
        } catch (IllegalStateException exception) {
            throw new InvalidPlanStateTransitionException(planId, oldStatus, action);
        }

        Plan saved = planRepository.save(plan);
        log.atInfo()
                .addKeyValue("planId", saved.getId())
                .addKeyValue("planCode", saved.getCode())
                .addKeyValue("oldStatus", oldStatus)
                .addKeyValue("newStatus", saved.getStatus())
                .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                .log("Changed plan status");
        return mapper.toResult(saved);
    }

    @FunctionalInterface
    private interface PlanStatusMutation {

        void apply(Plan plan);
    }
}
