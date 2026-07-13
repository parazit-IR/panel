package com.parazit.panel.application.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerServiceDisplayNamePolicyTest {

    private final CustomerServiceDisplayNamePolicy policy = new CustomerServiceDisplayNamePolicy();
    private final Instant now = Instant.parse("2026-07-13T00:00:00Z");

    @Test
    void prefersSafeProvisionUsername() {
        assertThat(policy.displayName(subscription("display"), provision("vpn_user_123"), "Monthly"))
                .isEqualTo("vpn_user_123");
    }

    @Test
    void avoidsEmailAddressAsServiceUsername() {
        assertThat(policy.displayName(subscription("customer display"), provision("user@example.com"), "Monthly"))
                .isEqualTo("customer_display");
    }

    @Test
    void fallsBackToPlanNameWithoutFullUuid() {
        assertThat(policy.displayName(null, null, "یک ماهه ۳۰ گیگ"))
                .startsWith("یک_ماهه_۳۰_گیگ")
                .doesNotContain("-");
    }

    private Subscription subscription(String displayName) {
        return Subscription.activate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                UUID.randomUUID(),
                "b".repeat(64),
                "abcdef",
                now,
                now.plusSeconds(3600),
                displayName,
                "1"
        );
    }

    private XuiClientProvision provision(String remoteEmail) {
        return XuiClientProvision.createPending(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                UUID.randomUUID().toString(),
                remoteEmail,
                "remote-sub",
                10_000L,
                now.plusSeconds(3600),
                1,
                now
        );
    }
}
