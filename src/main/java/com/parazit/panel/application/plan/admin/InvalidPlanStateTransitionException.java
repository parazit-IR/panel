package com.parazit.panel.application.plan.admin;

import com.parazit.panel.domain.plan.PlanStatus;
import java.util.UUID;

public class InvalidPlanStateTransitionException extends RuntimeException {

    public InvalidPlanStateTransitionException(UUID planId, PlanStatus currentStatus, String action) {
        super("Cannot " + action + " plan " + planId + " with status " + currentStatus);
    }
}
