package com.parazit.panel.application.plan.selection;

public class PlanSelectionConflictException extends RuntimeException {

    public PlanSelectionConflictException(Long telegramUserId) {
        super("Concurrent plan selection conflict for telegramUserId " + telegramUserId);
    }
}
