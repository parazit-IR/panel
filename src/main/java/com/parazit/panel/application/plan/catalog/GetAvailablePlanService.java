package com.parazit.panel.application.plan.catalog;

import com.parazit.panel.application.plan.catalog.query.GetAvailablePlanByCodeQuery;
import com.parazit.panel.application.plan.catalog.query.GetAvailablePlanByIdQuery;
import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;
import com.parazit.panel.application.port.in.plan.catalog.GetAvailablePlanUseCase;
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
public class GetAvailablePlanService implements GetAvailablePlanUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetAvailablePlanService.class);

    private final PlanRepository planRepository;
    private final AvailablePlanResultMapper mapper;

    public GetAvailablePlanService(PlanRepository planRepository, AvailablePlanResultMapper mapper) {
        this.planRepository = Objects.requireNonNull(planRepository, "planRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public AvailablePlanResult getById(GetAvailablePlanByIdQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        UUID planId = Objects.requireNonNull(query.planId(), "planId must not be null");
        Plan plan = planRepository.findByIdAndStatus(planId, PlanStatus.ACTIVE)
                .orElseThrow(() -> new AvailablePlanNotFoundException(planId));

        log.atDebug()
                .addKeyValue("planId", plan.getId())
                .addKeyValue("planCode", plan.getCode())
                .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                .log("Retrieved available plan by id");
        return mapper.toResult(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public AvailablePlanResult getByCode(GetAvailablePlanByCodeQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        String normalizedCode = Plan.normalizeCode(query.code());
        Plan plan = planRepository.findByCodeAndStatus(normalizedCode, PlanStatus.ACTIVE)
                .orElseThrow(() -> new AvailablePlanNotFoundException(normalizedCode));

        log.atDebug()
                .addKeyValue("planId", plan.getId())
                .addKeyValue("planCode", plan.getCode())
                .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                .log("Retrieved available plan by code");
        return mapper.toResult(plan);
    }
}
