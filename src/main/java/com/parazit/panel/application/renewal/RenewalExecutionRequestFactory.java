package com.parazit.panel.application.renewal;

import com.parazit.panel.config.properties.RenewalProperties;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.RenewalSnapshot;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.renewal.RenewalExecutionRequest;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RenewalExecutionRequestFactory {

    private final RenewalExpiryCalculator expiryCalculator;
    private final RenewalProperties properties;

    public RenewalExecutionRequestFactory(RenewalExpiryCalculator expiryCalculator, RenewalProperties properties) {
        this.expiryCalculator = Objects.requireNonNull(expiryCalculator, "expiryCalculator must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public RenewalExecutionRequest create(Order order, Payment payment, Instant requestedAt) {
        Objects.requireNonNull(order, "order must not be null");
        Objects.requireNonNull(payment, "payment must not be null");
        Instant requiredRequestedAt = Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        RenewalSnapshot snapshot = Objects.requireNonNull(order.getRenewalSnapshot(), "renewalSnapshot must not be null");
        CurrencyCode currency = CurrencyCode.valueOf(order.getCurrency());
        Instant approvedAt = payment.getApprovedAt() == null ? requiredRequestedAt : payment.getApprovedAt();
        Instant proposedExpiryAt = expiryCalculator.proposedExpiry(
                snapshot.currentExpiryAt(),
                snapshot.renewalDuration(),
                properties.expiryPolicy(),
                requiredRequestedAt
        );
        return new RenewalExecutionRequest(
                order.getId(),
                payment.getId(),
                order.getUserId(),
                snapshot.targetSubscriptionId(),
                snapshot.targetProvisionId(),
                snapshot.sourcePlanId(),
                snapshot.serviceUsername(),
                snapshot.trafficPolicy(),
                properties.expiryPolicy(),
                snapshot.currentExpiryAt(),
                proposedExpiryAt,
                snapshot.currentTrafficLimitBytes(),
                snapshot.currentUsedTrafficBytes(),
                snapshot.renewalTrafficBytes(),
                new Money(order.getFinalAmount(), currency),
                currency,
                approvedAt,
                requiredRequestedAt,
                RenewalOutboxPayloadVersions.RENEWAL_EXECUTION_REQUEST_V1
        );
    }
}
