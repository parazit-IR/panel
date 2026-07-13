package com.parazit.panel.application.renewal;

import com.parazit.panel.domain.order.RenewalExpiryPolicy;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RenewalExpiryCalculator {

    public Instant proposedExpiry(Instant currentExpiryAt, Duration renewalDuration, RenewalExpiryPolicy policy, Instant now) {
        Duration duration = Objects.requireNonNull(renewalDuration, "renewalDuration must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("renewalDuration must be positive");
        }
        RenewalExpiryPolicy effectivePolicy = Objects.requireNonNull(policy, "policy must not be null");
        Instant requiredNow = Objects.requireNonNull(now, "now must not be null");
        Instant base = switch (effectivePolicy) {
            case EXTEND_FROM_CURRENT_EXPIRY -> Objects.requireNonNull(currentExpiryAt, "currentExpiryAt must not be null");
            case EXTEND_FROM_NOW -> requiredNow;
            case EXTEND_FROM_LATER_OF_NOW_OR_EXPIRY -> currentExpiryAt == null || currentExpiryAt.isBefore(requiredNow)
                    ? requiredNow
                    : currentExpiryAt;
        };
        try {
            return base.plus(duration);
        } catch (ArithmeticException | DateTimeException exception) {
            throw new RenewalFlowException("telegram.renewal.cannot_renew");
        }
    }
}
