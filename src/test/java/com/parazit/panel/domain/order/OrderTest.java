package com.parazit.panel.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
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
}
