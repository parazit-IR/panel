package com.parazit.panel.application.plan.catalog;

import java.util.NoSuchElementException;
import java.util.UUID;

public class AvailablePlanNotFoundException extends NoSuchElementException {

    public AvailablePlanNotFoundException(UUID planId) {
        super("Available plan not found for id " + planId);
    }

    public AvailablePlanNotFoundException(String normalizedCode) {
        super("Available plan not found for code " + normalizedCode);
    }
}
