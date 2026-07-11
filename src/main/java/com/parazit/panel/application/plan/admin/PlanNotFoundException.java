package com.parazit.panel.application.plan.admin;

import java.util.NoSuchElementException;
import java.util.UUID;

public class PlanNotFoundException extends NoSuchElementException {

    public PlanNotFoundException(UUID planId) {
        super("Plan not found for id " + planId);
    }

    public PlanNotFoundException(String normalizedCode) {
        super("Plan not found for code " + normalizedCode);
    }
}
