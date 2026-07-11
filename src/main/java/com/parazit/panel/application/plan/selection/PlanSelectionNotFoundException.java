package com.parazit.panel.application.plan.selection;

import java.util.NoSuchElementException;

public class PlanSelectionNotFoundException extends NoSuchElementException {

    public PlanSelectionNotFoundException(Long telegramUserId) {
        super("Current plan selection not found for telegramUserId " + telegramUserId);
    }
}
