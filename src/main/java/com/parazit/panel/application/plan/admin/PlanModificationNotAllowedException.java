package com.parazit.panel.application.plan.admin;

import java.util.UUID;

public class PlanModificationNotAllowedException extends RuntimeException {

    public PlanModificationNotAllowedException(UUID planId) {
        super("Plan cannot be modified: " + planId);
    }
}
