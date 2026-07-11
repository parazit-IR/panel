package com.parazit.panel.application.plan.admin;

import com.parazit.panel.application.plan.PlanResultMapper;
import com.parazit.panel.application.plan.admin.query.GetPlanByCodeQuery;
import com.parazit.panel.application.plan.admin.query.GetPlanByIdQuery;
import com.parazit.panel.application.plan.result.PlanResult;
import com.parazit.panel.application.port.in.plan.admin.GetPlanUseCase;
import com.parazit.panel.common.exception.TraceIdFilter;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPlanService implements GetPlanUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetPlanService.class);

    private final PlanRepository planRepository;
    private final PlanResultMapper mapper;

    public GetPlanService(PlanRepository planRepository, PlanResultMapper mapper) {
        this.planRepository = Objects.requireNonNull(planRepository, "planRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public PlanResult getById(GetPlanByIdQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        UUID planId = Objects.requireNonNull(query.planId(), "planId must not be null");
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException(planId));

        log.atDebug()
                .addKeyValue("planId", plan.getId())
                .addKeyValue("planCode", plan.getCode())
                .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                .log("Retrieved plan by id");
        return mapper.toResult(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanResult getByCode(GetPlanByCodeQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        String normalizedCode = Plan.normalizeCode(query.code());
        Plan plan = planRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new PlanNotFoundException(normalizedCode));

        log.atDebug()
                .addKeyValue("planId", plan.getId())
                .addKeyValue("planCode", plan.getCode())
                .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                .log("Retrieved plan by code");
        return mapper.toResult(plan);
    }
}
