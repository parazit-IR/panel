package com.parazit.panel.application.renewal;

public record RenewalTrafficCalculation(long desiredTotalTrafficBytes, boolean resetUsage) {

    public RenewalTrafficCalculation {
        if (desiredTotalTrafficBytes < 0) {
            throw new IllegalArgumentException("desiredTotalTrafficBytes must be zero or positive");
        }
    }
}
