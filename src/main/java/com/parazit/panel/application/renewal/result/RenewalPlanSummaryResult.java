package com.parazit.panel.application.renewal.result;

import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.order.RenewalTrafficPolicy;
import java.time.Duration;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;

public record RenewalPlanSummaryResult(
        UUID planId,
        String name,
        String description,
        Duration duration,
        OptionalLong trafficBytes,
        OptionalInt maxDevices,
        Money price,
        RenewalTrafficPolicy trafficPolicy,
        boolean compatible
) {
}
