package com.parazit.panel.application.renewal;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.renewal.result.RenewalApplicationTarget;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.domain.order.RenewalSnapshot;
import com.parazit.panel.domain.renewal.RenewalExecutionRequest;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RenewalApplicationTargetFactory {

    private final RenewalExpiryCalculator expiryCalculator;
    private final RenewalTrafficCalculator trafficCalculator;
    private final SystemClockPort clock;

    public RenewalApplicationTargetFactory(
            RenewalExpiryCalculator expiryCalculator,
            RenewalTrafficCalculator trafficCalculator,
            SystemClockPort clock
    ) {
        this.expiryCalculator = Objects.requireNonNull(expiryCalculator, "expiryCalculator must not be null");
        this.trafficCalculator = Objects.requireNonNull(trafficCalculator, "trafficCalculator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public RenewalApplicationTarget create(
            UUID renewalOrderId,
            UUID targetSubscriptionId,
            XuiClientProvision provision,
            RenewalSnapshot snapshot,
            RenewalExecutionRequest executionRequest,
            XuiClientSnapshot remote
    ) {
        Objects.requireNonNull(renewalOrderId, "renewalOrderId must not be null");
        Objects.requireNonNull(targetSubscriptionId, "targetSubscriptionId must not be null");
        Objects.requireNonNull(provision, "provision must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(executionRequest, "executionRequest must not be null");
        Objects.requireNonNull(remote, "remote must not be null");
        Instant now = clock.now();
        Instant currentExpiry = remote.expiryTime() != null ? remote.expiryTime() : provision.getExpiresAt();
        Instant desiredExpiry = expiryCalculator.proposedExpiry(
                currentExpiry,
                snapshot.renewalDuration(),
                executionRequest.expiryPolicy(),
                now
        );
        if (currentExpiry != null && currentExpiry.isAfter(desiredExpiry)) {
            desiredExpiry = currentExpiry;
        }
        long used = Math.max(0, remote.totalConsumedBytes());
        RenewalTrafficCalculation traffic = trafficCalculator.calculate(
                snapshot.trafficPolicy(),
                remote.totalTrafficLimitBytes(),
                used,
                snapshot.renewalTrafficBytes()
        );
        return new RenewalApplicationTarget(
                renewalOrderId,
                targetSubscriptionId,
                provision.getId(),
                desiredExpiry,
                traffic.desiredTotalTrafficBytes(),
                traffic.resetUsage(),
                snapshot.trafficPolicy(),
                now,
                RenewalApplicationTarget.VERSION_V1
        );
    }
}
