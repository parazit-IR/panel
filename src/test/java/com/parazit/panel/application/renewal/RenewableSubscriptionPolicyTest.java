package com.parazit.panel.application.renewal;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.config.properties.RenewalProperties;
import com.parazit.panel.domain.order.RenewalExpiryPolicy;
import com.parazit.panel.domain.order.RenewalTrafficPolicy;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RenewableSubscriptionPolicyTest {

    private static final Instant NOW = Instant.parse("2026-07-14T00:00:00Z");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PROVISION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final RenewableSubscriptionPolicy policy = new RenewableSubscriptionPolicy(properties(true, true, false, true, true));

    @Test
    void acceptsActiveAndExpiredSubscriptionsWithSuccessfulProvision() {
        Subscription active = subscription(USER_ID, NOW.plus(Duration.ofDays(10)));
        XuiClientProvision activeProvision = activeProvision(USER_ID);
        Subscription expired = subscription(USER_ID, NOW.minus(Duration.ofDays(1)));
        expired.expire(NOW);

        assertThat(policy.evaluate(USER_ID, active, activeProvision, false, NOW).renewable()).isTrue();
        assertThat(policy.evaluate(USER_ID, expired, activeProvision, false, NOW).renewable()).isTrue();
    }

    @Test
    void rejectsWrongOwnerRevokedSuspendedMissingProvisionAndExistingOrder() {
        Subscription subscription = subscription(USER_ID, NOW.plus(Duration.ofDays(10)));
        XuiClientProvision provision = activeProvision(USER_ID);

        assertThat(policy.evaluate(UUID.randomUUID(), subscription, provision, false, NOW).reason())
                .isEqualTo(RenewalIneligibilityReason.OWNERSHIP_MISMATCH);
        assertThat(policy.evaluate(USER_ID, subscription, provision, true, NOW).reason())
                .isEqualTo(RenewalIneligibilityReason.ALREADY_HAS_ACTIVE_RENEWAL_ORDER);
        assertThat(policy.evaluate(USER_ID, subscription, null, false, NOW).reason())
                .isEqualTo(RenewalIneligibilityReason.PROVISION_NOT_FOUND);

        Subscription suspended = subscription(USER_ID, NOW.plus(Duration.ofDays(10)));
        suspended.suspend();
        assertThat(policy.evaluate(USER_ID, suspended, provision, false, NOW).reason())
                .isEqualTo(RenewalIneligibilityReason.SUBSCRIPTION_SUSPENDED);

        Subscription revoked = subscription(USER_ID, NOW.plus(Duration.ofDays(10)));
        revoked.revoke(NOW, "test");
        assertThat(policy.evaluate(USER_ID, revoked, provision, false, NOW).reason())
                .isEqualTo(RenewalIneligibilityReason.SUBSCRIPTION_REVOKED);
    }

    @Test
    void rejectsDisabledRenewalAndFailedProvision() {
        RenewableSubscriptionPolicy disabled = new RenewableSubscriptionPolicy(properties(false, true, false, true, true));
        Subscription subscription = subscription(USER_ID, NOW.plus(Duration.ofDays(10)));
        XuiClientProvision failed = activeProvision(USER_ID);
        failed.markOperationFailed("TEST", "safe");

        assertThat(disabled.evaluate(USER_ID, subscription, activeProvision(USER_ID), false, NOW).reason())
                .isEqualTo(RenewalIneligibilityReason.RENEWAL_DISABLED);
        assertThat(policy.evaluate(USER_ID, subscription, failed, false, NOW).reason())
                .isEqualTo(RenewalIneligibilityReason.PROVISIONING_FAILED);
    }

    private static Subscription subscription(UUID userId, Instant expiresAt) {
        return Subscription.activate(
                userId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                PROVISION_ID,
                101,
                UUID.randomUUID(),
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "0123456789abcdef",
                NOW.minus(Duration.ofDays(5)),
                expiresAt,
                "Work VPN",
                "v1"
        );
    }

    private static XuiClientProvision activeProvision(UUID userId) {
        XuiClientProvision provision = XuiClientProvision.createPending(
                userId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                101,
                UUID.randomUUID().toString(),
                "svc-1@example.invalid",
                "sub-1",
                30L * 1024 * 1024 * 1024,
                NOW.plus(Duration.ofDays(30)),
                1,
                NOW.minus(Duration.ofDays(1))
        );
        ReflectionTestUtils.setField(provision, "id", PROVISION_ID);
        provision.markProvisioning();
        provision.markActive(NOW.minus(Duration.ofHours(1)));
        return provision;
    }

    private static RenewalProperties properties(
            boolean enabled,
            boolean allowActive,
            boolean allowSuspended,
            boolean requireSuccessfulProvision,
            boolean requireRemoteClientReference
    ) {
        return new RenewalProperties(
                enabled,
                Duration.ofMinutes(30),
                Duration.ofMinutes(15),
                5,
                5,
                allowActive,
                true,
                allowSuspended,
                RenewalTrafficPolicy.RESET_TO_PLAN_LIMIT,
                RenewalExpiryPolicy.EXTEND_FROM_LATER_OF_NOW_OR_EXPIRY,
                requireSuccessfulProvision,
                requireRemoteClientReference,
                1
        );
    }
}
