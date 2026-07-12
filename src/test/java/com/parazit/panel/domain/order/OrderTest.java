package com.parazit.panel.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void createNormalizesCurrencyAndStartsCreated() {
        UUID userId = UUID.randomUUID();

        Order order = Order.create(userId, 500_000L, " irt ");

        assertThat(order.getUserId()).isEqualTo(userId);
        assertThat(order.getAmount()).isEqualTo(500_000L);
        assertThat(order.getCurrency()).isEqualTo("IRT");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void rejectsInvalidValues() {
        assertThatThrownBy(() -> Order.create(null, 1L, "IRT"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Order.create(UUID.randomUUID(), -1L, "IRT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
        assertThatThrownBy(() -> Order.create(UUID.randomUUID(), 1L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void paidProvisioningCompletedAndFailureRetryTransitionsAreControlled() {
        UUID userId = UUID.randomUUID();
        Order order = Order.createForPlanSelection(
                userId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                500_000L,
                "IRT"
        );
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        order.markPaymentPending();
        order.markPaid(now);
        order.markProvisioning(now.plusSeconds(1));
        order.markProvisioningFailed("XUI_TIMEOUT", "temporary");
        order.markProvisioning(now.plusSeconds(2));
        order.markCompleted(now.plusSeconds(3));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getPaidAt()).isEqualTo(now);
        assertThat(order.getCompletedAt()).isEqualTo(now.plusSeconds(3));
        assertThat(order.requiresProvisioning()).isTrue();
    }
}
