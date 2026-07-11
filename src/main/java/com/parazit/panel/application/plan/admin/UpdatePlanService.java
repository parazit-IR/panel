package com.parazit.panel.application.plan.admin;

import com.parazit.panel.application.plan.PlanResultMapper;
import com.parazit.panel.application.plan.admin.command.UpdatePlanCommand;
import com.parazit.panel.application.plan.result.PlanResult;
import com.parazit.panel.application.port.in.plan.admin.UpdatePlanUseCase;
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
public class UpdatePlanService implements UpdatePlanUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdatePlanService.class);

    private final PlanRepository planRepository;
    private final PlanResultMapper mapper;

    public UpdatePlanService(PlanRepository planRepository, PlanResultMapper mapper) {
        this.planRepository = Objects.requireNonNull(planRepository, "planRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public PlanResult update(UpdatePlanCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        UUID planId = Objects.requireNonNull(command.planId(), "planId must not be null");
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException(planId));
        if (plan.getStatus() == PlanStatus.ARCHIVED) {
            throw new PlanModificationNotAllowedException(planId);
        }

        plan.updateDetails(
                command.name(),
                command.description(),
                command.type(),
                command.priceAmount(),
                command.currency(),
                command.durationDays(),
                command.trafficLimitBytes(),
                command.maxDevices(),
                command.displayOrder()
        );
        Plan saved = planRepository.save(plan);
        log.atInfo()
                .addKeyValue("planId", saved.getId())
                .addKeyValue("planCode", saved.getCode())
                .addKeyValue("status", saved.getStatus())
                .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                .log("Updated plan details");

        return mapper.toResult(saved);
    }
}
