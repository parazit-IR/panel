package com.parazit.panel.application.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.customer.result.CustomerServiceStatus;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerServiceStatusMapperTest {

    private final CustomerServiceStatusMapper mapper = new CustomerServiceStatusMapper();
    private final Instant now = Instant.parse("2026-07-13T00:00:00Z");

    @Test
    void mapsActiveProvisionToActive() {
        XuiClientProvision provision = provision();
        provision.markProvisioning();
        provision.markActive(now);

        assertThat(mapper.map(subscription(now.plusSeconds(3600)), provision, now))
                .isEqualTo(CustomerServiceStatus.ACTIVE);
    }

    @Test
    void expiryTakesPrecedenceOverActiveProvision() {
        XuiClientProvision provision = provision();
        provision.markProvisioning();
        provision.markActive(now.minusSeconds(60));

        assertThat(mapper.map(subscription(now.minusSeconds(1)), provision, now))
                .isEqualTo(CustomerServiceStatus.EXPIRED);
    }

    @Test
    void mapsTerminalAndProvisioningStates() {
        Subscription revoked = subscription(now.plusSeconds(3600));
        revoked.revoke(now, "customer requested");
        assertThat(mapper.map(revoked, provision(), now)).isEqualTo(CustomerServiceStatus.REVOKED);

        assertThat(mapper.map(subscription(now.plusSeconds(3600)), provision(), now))
                .isEqualTo(CustomerServiceStatus.PROVISIONING);
    }

    private Subscription subscription(Instant expiresAt) {
        return Subscription.activate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                UUID.randomUUID(),
                "a".repeat(64),
                "abcdef",
                now.minusSeconds(60),
                expiresAt,
                "service",
                "1"
        );
    }

    private XuiClientProvision provision() {
        return XuiClientProvision.createPending(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                UUID.randomUUID().toString(),
                "vpn_user_1",
                "remote-sub",
                10_000L,
                now.plusSeconds(3600),
                1,
                now.minusSeconds(60)
        );
    }
}
