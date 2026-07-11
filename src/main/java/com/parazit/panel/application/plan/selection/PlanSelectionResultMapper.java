package com.parazit.panel.application.plan.selection;

import com.parazit.panel.application.plan.selection.result.PlanSelectionResult;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.user.User;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PlanSelectionResultMapper {

    public PlanSelectionResult toResult(User user, PlanSelection selection, boolean newlySelected) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(selection, "selection must not be null");
        return new PlanSelectionResult(
                selection.getId(),
                selection.getUserId(),
                user.getTelegramUserId(),
                selection.getPlanId(),
                selection.getPlanCodeSnapshot(),
                selection.getPlanNameSnapshot(),
                selection.getPlanTypeSnapshot(),
                selection.getPriceAmountSnapshot(),
                selection.getCurrencySnapshot(),
                selection.getDurationDaysSnapshot(),
                selection.getTrafficLimitBytesSnapshot(),
                selection.getMaxDevicesSnapshot(),
                selection.getStatus(),
                selection.getSelectedAt(),
                selection.getExpiresAt(),
                newlySelected
        );
    }
}
