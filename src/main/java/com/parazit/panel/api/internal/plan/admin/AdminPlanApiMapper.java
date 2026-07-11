package com.parazit.panel.api.internal.plan.admin;

import com.parazit.panel.application.plan.admin.command.ChangePlanStatusCommand;
import com.parazit.panel.application.plan.admin.command.CreatePlanCommand;
import com.parazit.panel.application.plan.admin.command.UpdatePlanCommand;
import com.parazit.panel.application.plan.admin.query.GetPlanByCodeQuery;
import com.parazit.panel.application.plan.admin.query.GetPlanByIdQuery;
import com.parazit.panel.application.plan.admin.query.ListPlansQuery;
import com.parazit.panel.application.plan.result.PlanResult;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AdminPlanApiMapper {

    public CreatePlanCommand toCommand(CreatePlanRequest request) {
        return new CreatePlanCommand(
                request.code(),
                request.name(),
                request.description(),
                request.type(),
                request.priceAmount(),
                request.currency(),
                request.durationDays(),
                request.trafficLimitBytes(),
                request.maxDevices(),
                request.displayOrder()
        );
    }

    public GetPlanByIdQuery toGetByIdQuery(UUID planId) {
        return new GetPlanByIdQuery(planId);
    }

    public GetPlanByCodeQuery toGetByCodeQuery(String code) {
        return new GetPlanByCodeQuery(code);
    }

    public ListPlansQuery toListQuery(PlanStatus status, PlanType type) {
        return new ListPlansQuery(status, type);
    }

    public UpdatePlanCommand toCommand(UUID planId, UpdatePlanRequest request) {
        return new UpdatePlanCommand(
                planId,
                request.name(),
                request.description(),
                request.type(),
                request.priceAmount(),
                request.currency(),
                request.durationDays(),
                request.trafficLimitBytes(),
                request.maxDevices(),
                request.displayOrder()
        );
    }

    public ChangePlanStatusCommand toChangeStatusCommand(UUID planId) {
        return new ChangePlanStatusCommand(planId);
    }

    public PlanResponse toResponse(PlanResult result) {
        return new PlanResponse(
                result.id(),
                result.code(),
                result.name(),
                result.description(),
                result.status(),
                result.type(),
                result.priceAmount(),
                result.currency(),
                result.durationDays(),
                result.trafficLimitBytes(),
                result.maxDevices(),
                result.displayOrder(),
                result.available(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    public List<PlanResponse> toResponse(List<PlanResult> results) {
        return results.stream()
                .map(this::toResponse)
                .toList();
    }
}
