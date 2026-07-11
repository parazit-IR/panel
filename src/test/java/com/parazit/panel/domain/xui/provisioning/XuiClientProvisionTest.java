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

    @Test
    void disablesAndDeletesWithTimestampsAndPreservesRemoteIdentity() {
        XuiClientProvision provision = activeProvision();
        String remoteClientId = provision.getRemoteClientId();
        String remoteEmail = provision.getRemoteEmail();

        provision.markDisabling();
        assertThat(provision.getStatus()).isEqualTo(XuiProvisionStatus.DISABLING);
        provision.markDisabled(NOW.plusSeconds(2));
        assertThat(provision.getStatus()).isEqualTo(XuiProvisionStatus.DISABLED);
        assertThat(provision.getDisabledAt()).isEqualTo(NOW.plusSeconds(2));
        provision.markDeleting();
        provision.markDeleted(NOW.plusSeconds(3));

        assertThat(provision.getStatus()).isEqualTo(XuiProvisionStatus.DELETED);
        assertThat(provision.getDeletedAt()).isEqualTo(NOW.plusSeconds(3));
        assertThat(provision.getRemoteClientId()).isEqualTo(remoteClientId);
        assertThat(provision.getRemoteEmail()).isEqualTo(remoteEmail);
    }

    @Test
    void forceDeleteCanMoveActiveProvisionToDeleted() {
        XuiClientProvision provision = activeProvision();

        provision.markDeleting();
        provision.markDeleted(NOW.plusSeconds(2));

        assertThat(provision.getStatus()).isEqualTo(XuiProvisionStatus.DELETED);
    }

    @Test
    void lifecycleRejectsInvalidTransitionsFromPendingAndProvisioning() {
        XuiClientProvision pending = provision();
        assertThatThrownBy(() -> pending.markDisabled(NOW.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(pending::markDeleting)
                .isInstanceOf(IllegalStateException.class);

        XuiClientProvision provisioning = provision();
        provisioning.markProvisioning();
        assertThatThrownBy(provisioning::markDisabling)
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(provisioning::markDeleting)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deletedProvisionIsTerminalForLifecycleFailures() {
        XuiClientProvision provision = activeProvision();
        provision.markDeleting();
        provision.markDeleted(NOW.plusSeconds(2));

        provision.markDeleting();
        provision.markDeleted(NOW.plusSeconds(3));

        assertThat(provision.getDeletedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThatThrownBy(() -> provision.markOperationFailed("FAILED", "no"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void operationUnknownAndFailureUseBoundedSafeMessages() {
        XuiClientProvision provision = activeProvision();
        provision.markDisabling();

        provision.markOperationUnknown("TIMEOUT", "x".repeat(800));
        assertThat(provision.getStatus()).isEqualTo(XuiProvisionStatus.UNKNOWN);
        assertThat(provision.getFailureMessage()).hasSize(500);

        provision.markOperationFailed("REMOTE_REJECTED", "failed");
        assertThat(provision.getStatus()).isEqualTo(XuiProvisionStatus.FAILED);
        assertThat(provision.getFailureCode()).isEqualTo("REMOTE_REJECTED");
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

    private static XuiClientProvision activeProvision() {
        XuiClientProvision provision = provision();
        provision.markProvisioning();
        provision.markActive(NOW.plusSeconds(1));
        return provision;
    }
}
