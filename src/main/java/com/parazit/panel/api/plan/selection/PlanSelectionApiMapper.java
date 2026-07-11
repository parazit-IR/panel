package com.parazit.panel.api.plan.selection;

import com.parazit.panel.application.plan.selection.command.ClearPlanSelectionCommand;
import com.parazit.panel.application.plan.selection.command.SelectPlanCommand;
import com.parazit.panel.application.plan.selection.query.GetCurrentPlanSelectionQuery;
import com.parazit.panel.application.plan.selection.result.PlanSelectionResult;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PlanSelectionApiMapper {

    public SelectPlanCommand toCommand(Long telegramUserId, SelectPlanRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return new SelectPlanCommand(telegramUserId, request.planId());
    }

    public GetCurrentPlanSelectionQuery toGetCurrentQuery(Long telegramUserId) {
        return new GetCurrentPlanSelectionQuery(telegramUserId);
    }

    public ClearPlanSelectionCommand toClearCommand(Long telegramUserId) {
        return new ClearPlanSelectionCommand(telegramUserId);
    }

    public PlanSelectionResponse toResponse(PlanSelectionResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new PlanSelectionResponse(
                result.selectionId(),
                result.planId(),
                result.planCode(),
                result.planName(),
                result.planType(),
                result.priceAmount(),
                result.currency(),
                result.durationDays(),
                result.trafficLimitBytes(),
                result.maxDevices(),
                result.status(),
                result.selectedAt(),
                result.expiresAt(),
                result.newlySelected()
        );
    }
}
