package com.parazit.panel.application.plan.admin;

import com.parazit.panel.application.plan.PlanResultMapper;
import com.parazit.panel.application.plan.admin.query.ListPlansQuery;
import com.parazit.panel.application.plan.result.PlanResult;
import com.parazit.panel.application.port.in.plan.admin.ListPlansUseCase;
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
public class ListPlansService implements ListPlansUseCase {

    private static final Logger log = LoggerFactory.getLogger(ListPlansService.class);

    private final PlanRepository planRepository;
    private final PlanResultMapper mapper;

    public ListPlansService(PlanRepository planRepository, PlanResultMapper mapper) {
        this.planRepository = Objects.requireNonNull(planRepository, "planRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanResult> list(ListPlansQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        PlanStatus status = query.status();
        PlanType type = query.type();

        List<Plan> plans;
        if (status != null && type != null) {
            plans = planRepository.findAllByStatusAndTypeOrderByDisplayOrderAscCodeAsc(status, type);
        } else if (status != null) {
            plans = planRepository.findAllByStatusOrderByDisplayOrderAscCodeAsc(status);
        } else if (type != null) {
            plans = planRepository.findAllByTypeOrderByDisplayOrderAscCodeAsc(type);
        } else {
            plans = planRepository.findAllOrderByDisplayOrderAscCodeAsc();
        }

        log.atDebug()
                .addKeyValue("status", status)
                .addKeyValue("type", type)
                .addKeyValue("count", plans.size())
                .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                .log("Listed admin plans");
        return plans.stream()
                .map(mapper::toResult)
                .toList();
    }
}
