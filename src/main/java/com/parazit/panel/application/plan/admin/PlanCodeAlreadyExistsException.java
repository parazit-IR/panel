package com.parazit.panel.application.plan.admin;

public class PlanCodeAlreadyExistsException extends RuntimeException {

    public PlanCodeAlreadyExistsException(String normalizedCode) {
        super("Plan code already exists: " + normalizedCode);
    }
}
