package com.parazit.panel.application.renewal;

import com.parazit.panel.domain.order.RenewalTrafficPolicy;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RenewalTrafficCalculator {

    public RenewalTrafficCalculation calculate(
            RenewalTrafficPolicy policy,
            long currentTotalTrafficBytes,
            long currentUsedTrafficBytes,
            Long renewalTrafficBytes
    ) {
        if (currentTotalTrafficBytes < 0 || currentUsedTrafficBytes < 0) {
            throw new IllegalArgumentException("current traffic values must be zero or positive");
        }
        RenewalTrafficPolicy requiredPolicy = Objects.requireNonNull(policy, "policy must not be null");
        return switch (requiredPolicy) {
            case RESET_TO_PLAN_LIMIT -> new RenewalTrafficCalculation(planLimitOrUnlimited(renewalTrafficBytes), true);
            case ADD_TO_REMAINING -> new RenewalTrafficCalculation(addToRemaining(currentTotalTrafficBytes, currentUsedTrafficBytes, renewalTrafficBytes), true);
            case ADD_TO_TOTAL_LIMIT -> new RenewalTrafficCalculation(addToTotal(currentTotalTrafficBytes, renewalTrafficBytes), false);
            case UNCHANGED -> new RenewalTrafficCalculation(currentTotalTrafficBytes, false);
        };
    }

    private static long addToRemaining(long currentTotalTrafficBytes, long currentUsedTrafficBytes, Long renewalTrafficBytes) {
        if (isUnlimited(currentTotalTrafficBytes) || renewalTrafficBytes == null) {
            return 0;
        }
        long remaining = Math.max(0, currentTotalTrafficBytes - Math.min(currentUsedTrafficBytes, currentTotalTrafficBytes));
        return Math.addExact(remaining, renewalTrafficBytes);
    }

    private static long addToTotal(long currentTotalTrafficBytes, Long renewalTrafficBytes) {
        if (isUnlimited(currentTotalTrafficBytes) || renewalTrafficBytes == null) {
            return 0;
        }
        return Math.addExact(currentTotalTrafficBytes, renewalTrafficBytes);
    }

    private static long planLimitOrUnlimited(Long renewalTrafficBytes) {
        return renewalTrafficBytes == null ? 0 : renewalTrafficBytes;
    }

    private static boolean isUnlimited(long trafficBytes) {
        return trafficBytes == 0;
    }
}
