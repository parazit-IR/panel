package com.parazit.panel.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.domain.plan.CurrencyCode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RenewalOrderTest {

    @Test
    void createsRenewalOrderWithSnapshotAndNoProvisioningRequirement() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID selectionId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        RenewalSnapshot snapshot = snapshot(subscriptionId, planId, 500_000);

        Order order = Order.createRenewal(userId, planId, selectionId, subscriptionId, snapshot, 500_000, "IRT");

        assertThat(order.getType()).isEqualTo(OrderType.RENEWAL);
        assertThat(order.isRenewal()).isTrue();
        assertThat(order.requiresProvisioning()).isFalse();
        assertThat(order.getTargetSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(order.getRenewalSnapshot()).isEqualTo(snapshot);
        assertThatThrownBy(() -> order.markProvisioning(Instant.parse("2026-07-14T00:00:00Z")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsRenewalOrderWhenSnapshotDoesNotMatchTargetPlanOrAmount() {
        UUID subscriptionId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        assertThatThrownBy(() -> Order.createRenewal(UUID.randomUUID(), planId, UUID.randomUUID(), UUID.randomUUID(), snapshot(subscriptionId, planId, 500_000), 500_000, "IRT"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Order.createRenewal(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), subscriptionId, snapshot(subscriptionId, planId, 500_000), 500_000, "IRT"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Order.createRenewal(UUID.randomUUID(), planId, UUID.randomUUID(), subscriptionId, snapshot(subscriptionId, planId, 500_000), 400_000, "IRT"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static RenewalSnapshot snapshot(UUID subscriptionId, UUID planId, long amount) {
        return new RenewalSnapshot(
                subscriptionId,
                UUID.randomUUID(),
                "Work VPN",
                "svc_001",
                Instant.parse("2026-07-20T00:00:00Z"),
                30L * 1024 * 1024 * 1024,
                12L * 1024 * 1024 * 1024,
                Duration.ofDays(30),
                30L * 1024 * 1024 * 1024,
                RenewalTrafficPolicy.RESET_TO_PLAN_LIMIT,
                new Money(amount, CurrencyCode.IRT),
                new Money(amount, CurrencyCode.IRT),
                "30 Days",
                "Renewal",
                planId,
                Instant.parse("2026-07-14T00:00:00Z")
        );
    }
}
