package com.parazit.panel.application.plan.catalog;

import com.parazit.panel.application.plan.catalog.query.ListAvailablePlansQuery;
import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;
import com.parazit.panel.application.port.in.plan.catalog.ListAvailablePlansUseCase;
import com.parazit.panel.common.exception.TraceIdFilter;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListAvailablePlansService implements ListAvailablePlansUseCase {

    private static final Logger log = LoggerFactory.getLogger(ListAvailablePlansService.class);

    private final PlanRepository planRepository;
    private final AvailablePlanResultMapper mapper;

    public ListAvailablePlansService(PlanRepository planRepository, AvailablePlanResultMapper mapper) {
        this.planRepository = Objects.requireNonNull(planRepository, "planRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailablePlanResult> list(ListAvailablePlansQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        PlanType type = query.type();

        List<Plan> plans = type == null
                ? planRepository.findAllByStatusOrderByDisplayOrderAscCodeAsc(PlanStatus.ACTIVE)
                : planRepository.findAllByStatusAndTypeOrderByDisplayOrderAscCodeAsc(PlanStatus.ACTIVE, type);

        log.atDebug()
                .addKeyValue("type", type)
                .addKeyValue("count", plans.size())
                .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                .log("Listed available plans");

        return plans.stream()
                .map(mapper::toResult)
                .toList();
    }
}
