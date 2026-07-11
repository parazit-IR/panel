package com.parazit.panel.application.plan.selection;

public class UserNotEligibleForPlanSelectionException extends RuntimeException {

    public UserNotEligibleForPlanSelectionException(Long telegramUserId) {
        super("User is not eligible for plan selection: " + telegramUserId);
    }
}
