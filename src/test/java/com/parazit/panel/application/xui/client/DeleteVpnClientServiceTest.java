package com.parazit.panel.application.xui.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.xui.XuiClientManagementClient;
import com.parazit.panel.application.xui.client.command.DeleteVpnClientCommand;
import com.parazit.panel.application.xui.client.model.DeleteXuiClientRequest;
import com.parazit.panel.application.xui.client.model.DeleteXuiClientResponse;
import com.parazit.panel.application.xui.client.result.XuiClientLifecycleResult;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.config.properties.XuiClientProvisioningProperties;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DeleteVpnClientServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");
    private static final UUID PROVISION_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private XuiClientLifecycleTransaction transaction;
    private XuiRemoteClientLookupService lookupService;
    private XuiClientManagementClient managementClient;
    private DeleteVpnClientService service;

    @BeforeEach
    void setUp() {
        transaction = org.mockito.Mockito.mock(XuiClientLifecycleTransaction.class);
        lookupService = org.mockito.Mockito.mock(XuiRemoteClientLookupService.class);
        managementClient = org.mockito.Mockito.mock(XuiClientManagementClient.class);
        SystemClockPort clock = () -> NOW;
        service = new DeleteVpnClientService(
                transaction,
                lookupService,
                managementClient,
                clock,
                new XuiClientProvisioningProperties("xtls-rprx-vision", 1),
                new XuiClientLifecycleResultMapper()
        );
    }

    @Test
    void deletesDisabledProvisionAndMarksDeleted() {
        XuiClientProvision deleting = disabledProvision();
        deleting.markDeleting();
        XuiClientProvision deleted = disabledProvision();
        deleted.markDeleting();
        deleted.markDeleted(NOW);
        when(transaction.prepareDelete(123L, PROVISION_ID, false))
                .thenReturn(new PreparedXuiClientLifecycleOperation(deleting, true, false));
        when(lookupService.findVerified(deleting))
                .thenReturn(Optional.of(snapshot()))
                .thenReturn(Optional.empty());
        when(managementClient.deleteClient(any(DeleteXuiClientRequest.class)))
                .thenReturn(new DeleteXuiClientResponse(7, snapshot().clientId(), true, false, "ok"));
        when(transaction.find(PROVISION_ID)).thenReturn(deleting);
        when(transaction.markDeleted(PROVISION_ID, NOW)).thenReturn(deleted);

        XuiClientLifecycleResult result = service.delete(new DeleteVpnClientCommand(123L, PROVISION_ID, false));

        assertThat(result.status()).isEqualTo(XuiProvisionStatus.DELETED);
        assertThat(result.changed()).isTrue();
        verify(managementClient).deleteClient(any(DeleteXuiClientRequest.class));
    }

    @Test
    void alreadyDeletedReturnsIdempotentlyWithoutRemoteCall() {
        XuiClientProvision deleted = disabledProvision();
        deleted.markDeleting();
        deleted.markDeleted(NOW);
        when(transaction.prepareDelete(123L, PROVISION_ID, false))
                .thenReturn(new PreparedXuiClientLifecycleOperation(deleted, false, true));

        XuiClientLifecycleResult result = service.delete(new DeleteVpnClientCommand(123L, PROVISION_ID, false));

        assertThat(result.changed()).isFalse();
        assertThat(result.status()).isEqualTo(XuiProvisionStatus.DELETED);
        verify(managementClient, never()).deleteClient(any());
    }

    @Test
    void activeWithoutForceIsRejectedBeforeRemoteCall() {
        when(transaction.prepareDelete(123L, PROVISION_ID, false))
                .thenThrow(new XuiClientDeleteNotAllowedException("Active Xui client requires force delete"));

        assertThatThrownBy(() -> service.delete(new DeleteVpnClientCommand(123L, PROVISION_ID, false)))
                .isInstanceOf(XuiClientDeleteNotAllowedException.class);
        verify(managementClient, never()).deleteClient(any());
    }

    private static XuiClientProvision disabledProvision() {
        XuiClientProvision provision = XuiClientProvision.createPending(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                7,
                "11111111-1111-1111-1111-111111111111",
                "vpn_abc_def",
                "sub123",
                1024,
                NOW.plusSeconds(86_400),
                2,
                NOW
        );
        ReflectionTestUtils.setField(provision, "id", PROVISION_ID);
        provision.markProvisioning();
        provision.markActive(NOW.minusSeconds(120));
        provision.markDisabling();
        provision.markDisabled(NOW.minusSeconds(60));
        return provision;
    }

    private static XuiClientSnapshot snapshot() {
        return new XuiClientSnapshot(
                "11111111-1111-1111-1111-111111111111",
                "vpn_abc_def",
                false,
                1024,
                0,
                0,
                NOW.plusSeconds(86_400),
                2,
                "sub123",
                "xtls-rprx-vision"
        );
    }
}
