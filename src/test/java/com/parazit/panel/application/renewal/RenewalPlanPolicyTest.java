package com.parazit.panel.application.renewal;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.sales.SalesAvailabilityService;
import com.parazit.panel.config.properties.ManualPaymentProperties;
import com.parazit.panel.config.properties.SalesControlProperties;
import com.parazit.panel.config.properties.WalletPaymentProperties;
import com.parazit.panel.config.properties.ZarinpalProperties;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RenewalPlanPolicyTest {

    private static final Instant NOW = Instant.parse("2026-07-14T00:00:00Z");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID PROVISION_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");

    @Test
    void planMustBeActiveAndExplicitlyRenewalEnabled() {
        RenewalPlanEligibilityPolicy policy = new RenewalPlanEligibilityPolicy(sales(true));
        Plan plan = Plan.create("REN30", "30 Days", "Renewal", PlanType.TRAFFIC_LIMITED, 500_000, CurrencyCode.IRT, 30, 30L * 1024 * 1024 * 1024, 1, 1);

        assertThat(policy.eligible(plan)).isFalse();

        plan.enableRenewal();
        assertThat(policy.eligible(plan)).isFalse();

        plan.activate();
        assertThat(policy.eligible(plan)).isTrue();
    }

    @Test
    void salesControlCanDisableRenewalPlanEligibility() {
        RenewalPlanEligibilityPolicy policy = new RenewalPlanEligibilityPolicy(sales(false));
        Plan plan = Plan.create("REN50", "50 Days", "Renewal", PlanType.TRAFFIC_LIMITED, 700_000, CurrencyCode.IRT, 30, 50L * 1024 * 1024 * 1024, 1, 1);
        plan.enableRenewal();
        plan.activate();

        assertThat(policy.eligible(plan)).isFalse();
    }

    @Test
    void compatibilityRequiresSameProvisionAndRemoteClientReference() {
        RenewalPlanCompatibilityPolicy policy = new RenewalPlanCompatibilityPolicy();
        Subscription subscription = subscription();
        XuiClientProvision provision = provision(USER_ID, PROVISION_ID, UUID.randomUUID().toString());
        Plan plan = Plan.create("REN60", "60 Days", "Renewal", PlanType.TRAFFIC_LIMITED, 900_000, CurrencyCode.IRT, 60, 60L * 1024 * 1024 * 1024, 1, 1);

        assertThat(policy.compatible(subscription, provision, plan)).isTrue();
        assertThat(policy.compatible(subscription, provision(UUID.randomUUID(), PROVISION_ID, UUID.randomUUID().toString()), plan)).isFalse();
        assertThat(policy.compatible(subscription, provision(USER_ID, UUID.randomUUID(), UUID.randomUUID().toString()), plan)).isFalse();
    }

    private static SalesAvailabilityService sales(boolean renewalEnabled) {
        return new SalesAvailabilityService(
                new SalesControlProperties(true, renewalEnabled, false, false, false, false, false, false, "", "", "", null),
                new ManualPaymentProperties(false, Duration.ofMinutes(30), 1000, 9999, 10, "", "", "", "", true, Duration.ofMinutes(2)),
                new ZarinpalProperties(false, "", URI.create("https://api.zarinpal.com"), URI.create("https://www.zarinpal.com/pg/StartPay"), "/request", "/verify", URI.create("http://localhost/callback"), URI.create("http://localhost/success"), URI.create("http://localhost/fail"), URI.create("http://localhost/cancel"), Duration.ofSeconds(1), Duration.ofSeconds(1), 0, Duration.ofMillis(100), true, true),
                new WalletPaymentProperties(false, true, true, CurrencyCode.IRT, 0, 0, 3, Duration.ofMinutes(15)),
                List.of(),
                () -> NOW
        );
    }

    private static Subscription subscription() {
        return Subscription.activate(
                USER_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                PROVISION_ID,
                101,
                UUID.randomUUID(),
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "0123456789abcdef",
                NOW.minus(Duration.ofDays(5)),
                NOW.plus(Duration.ofDays(10)),
                "Work VPN",
                "v1"
        );
    }

    private static XuiClientProvision provision(UUID userId, UUID provisionId, String remoteClientId) {
        XuiClientProvision provision = XuiClientProvision.createPending(
                userId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                101,
                remoteClientId,
                "svc-2@example.invalid",
                "sub-2",
                30L * 1024 * 1024 * 1024,
                NOW.plus(Duration.ofDays(30)),
                1,
                NOW.minus(Duration.ofDays(1))
        );
        ReflectionTestUtils.setField(provision, "id", provisionId);
        provision.markProvisioning();
        provision.markActive(NOW.minus(Duration.ofHours(1)));
        return provision;
    }
}
