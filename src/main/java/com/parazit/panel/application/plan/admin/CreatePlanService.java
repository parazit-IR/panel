package com.parazit.panel.application.plan.admin;

import com.parazit.panel.application.plan.PlanResultMapper;
import com.parazit.panel.application.plan.admin.command.CreatePlanCommand;
import com.parazit.panel.application.plan.result.PlanResult;
import com.parazit.panel.application.port.in.plan.admin.CreatePlanUseCase;
import com.parazit.panel.common.exception.TraceIdFilter;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePlanService implements CreatePlanUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreatePlanService.class);

    private final PlanRepository planRepository;
    private final PlanResultMapper mapper;

    public CreatePlanService(PlanRepository planRepository, PlanResultMapper mapper) {
        this.planRepository = Objects.requireNonNull(planRepository, "planRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public PlanResult create(CreatePlanCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String normalizedCode = Plan.normalizeCode(command.code());
        if (planRepository.existsByCode(normalizedCode)) {
            throw new PlanCodeAlreadyExistsException(normalizedCode);
        }

        Plan plan = Plan.create(
                normalizedCode,
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

        try {
            Plan saved = planRepository.save(plan);
            log.atInfo()
                    .addKeyValue("planId", saved.getId())
                    .addKeyValue("planCode", saved.getCode())
                    .addKeyValue("status", saved.getStatus())
                    .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                    .log("Created plan");
            return mapper.toResult(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new PlanCodeAlreadyExistsException(normalizedCode);
        }
    }
}
