package com.parazit.panel.domain.xui.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class XuiClientProvisionTest {

    private static final Instant NOW = Instant.parse("2026-07-11T12:00:00Z");

    @Test
    void createsPendingProvisionAndTransitionsToActive() {
        XuiClientProvision provision = provision();

        assertThat(provision.getStatus()).isEqualTo(XuiProvisionStatus.PENDING);
        assertThat(provision.getProvisionedAt()).isNull();

        provision.markProvisioning();
        assertThat(provision.getStatus()).isEqualTo(XuiProvisionStatus.PROVISIONING);
        provision.markActive(NOW.plusSeconds(1));
        assertThat(provision.getStatus()).isEqualTo(XuiProvisionStatus.ACTIVE);
        assertThat(provision.getProvisionedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void validatesRequiredFields() {
        assertThatThrownBy(() -> XuiClientProvision.createPending(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0,
                UUID.randomUUID().toString(), "vpn_a_b", "sub", 0, NOW.plusSeconds(1), 0, NOW
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> XuiClientProvision.createPending(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
                "not-a-uuid", "vpn_a_b", "sub", 0, NOW.plusSeconds(1), 0, NOW
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> XuiClientProvision.createPending(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
                UUID.randomUUID().toString(), " ", "sub", -1, NOW.plusSeconds(1), 0, NOW
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> XuiClientProvision.createPending(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
                UUID.randomUUID().toString(), "vpn_a_b", "sub", 0, NOW, 0, NOW
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void marksFailedAndUnknownWithBoundedSafeMessage() {
        XuiClientProvision provision = provision();

        provision.markProvisioning();
        provision.markFailed("REMOTE_REJECTED", "x".repeat(800));
        assertThat(provision.getStatus()).isEqualTo(XuiProvisionStatus.FAILED);
        assertThat(provision.getFailureMessage()).hasSize(500);

        provision.markProvisioning();
        provision.markUnknown("TIMEOUT", "unknown");
        assertThat(provision.getStatus()).isEqualTo(XuiProvisionStatus.UNKNOWN);
        assertThat(provision.getFailureCode()).isEqualTo("TIMEOUT");
    }

    @Test
    void activeCannotBeFailed() {
        XuiClientProvision provision = provision();
        provision.markProvisioning();
        provision.markActive(NOW.plusSeconds(1));

        assertThatThrownBy(() -> provision.markFailed("FAILED", "no"))
                .isInstanceOf(IllegalStateException.class);
    }

    private static XuiClientProvision provision() {
        return XuiClientProvision.createPending(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                7,
                UUID.randomUUID().toString(),
                "vpn_a_b",
                "sub123",
                1024,
                NOW.plusSeconds(3600),
                2,
                NOW
        );
    }
}
