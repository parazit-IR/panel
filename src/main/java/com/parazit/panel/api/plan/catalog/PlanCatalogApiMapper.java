package com.parazit.panel.api.plan.catalog;

import com.parazit.panel.application.plan.catalog.query.GetAvailablePlanByCodeQuery;
import com.parazit.panel.application.plan.catalog.query.GetAvailablePlanByIdQuery;
import com.parazit.panel.application.plan.catalog.query.ListAvailablePlansQuery;
import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;
import com.parazit.panel.domain.plan.PlanType;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PlanCatalogApiMapper {

    public ListAvailablePlansQuery toListQuery(PlanType type) {
        return new ListAvailablePlansQuery(type);
    }

    public GetAvailablePlanByIdQuery toGetByIdQuery(UUID planId) {
        return new GetAvailablePlanByIdQuery(planId);
    }

    public GetAvailablePlanByCodeQuery toGetByCodeQuery(String code) {
        return new GetAvailablePlanByCodeQuery(code);
    }

    public AvailablePlanResponse toResponse(AvailablePlanResult result) {
        return new AvailablePlanResponse(
                result.id(),
                result.code(),
                result.name(),
                result.description(),
                result.type(),
                result.priceAmount(),
                result.currency(),
                result.durationDays(),
                result.trafficLimitBytes(),
                result.maxDevices()
        );
    }

    public List<AvailablePlanResponse> toResponse(List<AvailablePlanResult> results) {
        return results.stream()
                .map(this::toResponse)
                .toList();
    }
}
