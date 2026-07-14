package com.parazit.panel.application.wallet.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.parazit.panel.application.sales.SalesAvailabilityService;
import com.parazit.panel.config.properties.WalletPaymentProperties;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.plan.CurrencyCode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class WalletOrderPaymentEligibilityPolicyTest {

    private static final Instant NOW = Instant.parse("2026-07-14T00:00:00Z");

    private SalesAvailabilityService sales;
    private WalletOrderPaymentEligibilityPolicy policy;
    private UUID userId;

    @BeforeEach
    void setUp() {
        sales = org.mockito.Mockito.mock(SalesAvailabilityService.class);
        policy = new WalletOrderPaymentEligibilityPolicy(sales,
                new WalletPaymentProperties(true, true, true, CurrencyCode.IRT, 0, 0, 3, Duration.ofMinutes(15)));
        userId = UUID.randomUUID();
    }

    @Test
    void acceptsPayableOrderWhenFeatureIsEnabled() {
        when(sales.walletPaymentAvailable()).thenReturn(true);
        Order order = Order.create(userId, 100_000L, "IRT");
        ReflectionTestUtils.setField(order, "id", UUID.randomUUID());
        order.markPaymentPending();

        assertThat(policy.evaluate(userId, order, List.of(), NOW).eligible()).isTrue();
    }

    @Test
    void rejectsDisabledConflictingAndApprovedPaymentStates() {
        Order order = Order.create(userId, 100_000L, "IRT");
        ReflectionTestUtils.setField(order, "id", UUID.randomUUID());
        order.markPaymentPending();

        assertThat(policy.evaluate(userId, order, List.of(), NOW).reason())
                .isEqualTo(WalletOrderPaymentEligibilityReason.FEATURE_DISABLED);

        when(sales.walletPaymentAvailable()).thenReturn(true);
        Payment manual = Payment.create(order.getId(), userId, PaymentMethod.CARD_TO_CARD, 100_000L, 100_000L, "IRT", NOW.plus(Duration.ofMinutes(30)));
        manual.markWaitingForPayment();
        assertThat(policy.evaluate(userId, order, List.of(manual), NOW).reason())
                .isEqualTo(WalletOrderPaymentEligibilityReason.CONFLICTING_PAYMENT_EXISTS);

        Payment approved = Payment.create(order.getId(), userId, PaymentMethod.CARD_TO_CARD, 100_000L, 100_000L, "IRT", NOW.plus(Duration.ofMinutes(30)));
        approved.markWaitingForPayment();
        approved.markApproved(NOW, "tx", null);
        assertThat(policy.evaluate(userId, order, List.of(approved), NOW).reason())
                .isEqualTo(WalletOrderPaymentEligibilityReason.APPROVED_PAYMENT_EXISTS);
        assertThat(approved.getStatus()).isEqualTo(PaymentStatus.APPROVED);
    }
}
