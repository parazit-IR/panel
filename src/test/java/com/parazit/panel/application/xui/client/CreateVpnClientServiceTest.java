package com.parazit.panel.application.xui.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.xui.XuiClientManagementClient;
import com.parazit.panel.application.port.out.xui.XuiInboundClient;
import com.parazit.panel.application.xui.client.command.CreateVpnClientCommand;
import com.parazit.panel.application.xui.client.model.CreateXuiClientRequest;
import com.parazit.panel.application.xui.client.model.CreateXuiClientResponse;
import com.parazit.panel.application.xui.client.result.CreateVpnClientResult;
import com.parazit.panel.application.xui.inbound.XuiInboundEligibilityPolicy;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.application.xui.model.XuiInboundSnapshot;
import com.parazit.panel.config.properties.XuiClientProvisioningProperties;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CreateVpnClientServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PLAN_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID SELECTION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID PROVISION_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final String CLIENT_ID = "11111111-1111-1111-1111-111111111111";

    private PrepareXuiProvisionTransaction prepareTransaction;
    private UpdateXuiProvisionStatusTransaction statusTransaction;
    private XuiInboundClient inboundClient;
    private XuiClientManagementClient managementClient;
    private CreateVpnClientService service;

    @BeforeEach
    void setUp() {
        prepareTransaction = org.mockito.Mockito.mock(PrepareXuiProvisionTransaction.class);
        statusTransaction = org.mockito.Mockito.mock(UpdateXuiProvisionStatusTransaction.class);
        inboundClient = org.mockito.Mockito.mock(XuiInboundClient.class);
        managementClient = org.mockito.Mockito.mock(XuiClientManagementClient.class);
        SystemClockPort clock = () -> NOW;
        service = new CreateVpnClientService(
                prepareTransaction,
                statusTransaction,
                inboundClient,
                new XuiInboundEligibilityPolicy(),
                managementClient,
                clock,
                new XuiClientProvisioningProperties("xtls-rprx-vision", 1),
                new CreateVpnClientResultMapper()
        );
    }

    @Test
    void createsRemoteClientAndMarksActive() {
        XuiClientProvision pending = provision();
        XuiClientProvision provisioning = provision();
        provisioning.markProvisioning();
        XuiClientProvision active = provision();
        active.markProvisioning();
        active.markActive(NOW);
        when(inboundClient.getInboundById(7L)).thenReturn(Optional.of(inbound(7)));
        when(prepareTransaction.prepare(123L, SELECTION_ID, 7)).thenReturn(new PreparedXuiProvision(pending, true));
        when(statusTransaction.claimProvisioning(PROVISION_ID)).thenReturn(new ClaimedXuiProvision(provisioning, true));
        when(managementClient.createClient(any(CreateXuiClientRequest.class))).thenReturn(new CreateXuiClientResponse(
                7, CLIENT_ID, "vpn_user_provision", "sub123", true, "ok"
        ));
        when(statusTransaction.markActive(PROVISION_ID, NOW)).thenReturn(active);

        CreateVpnClientResult result = service.create(new CreateVpnClientCommand(123L, SELECTION_ID, 7L));

        assertThat(result.status()).isEqualTo(XuiProvisionStatus.ACTIVE);
        assertThat(result.newlyCreated()).isTrue();
        verify(managementClient).createClient(any(CreateXuiClientRequest.class));
    }

    @Test
    void returnsExistingActiveProvisionWithoutRemotePost() {
        XuiClientProvision active = provision();
        active.markProvisioning();
        active.markActive(NOW);
        when(inboundClient.getInboundById(7L)).thenReturn(Optional.of(inbound(7)));
        when(prepareTransaction.prepare(123L, SELECTION_ID, 7)).thenReturn(new PreparedXuiProvision(active, false));

        CreateVpnClientResult result = service.create(new CreateVpnClientCommand(123L, SELECTION_ID, 7L));

        assertThat(result.newlyCreated()).isFalse();
        assertThat(result.status()).isEqualTo(XuiProvisionStatus.ACTIVE);
        verify(managementClient, never()).createClient(any());
    }

    @Test
    void reconcilesTimeoutToActiveWhenRemoteClientExists() {
        XuiClientProvision pending = provision();
        XuiClientProvision provisioning = provision();
        provisioning.markProvisioning();
        XuiClientProvision unknown = provision();
        unknown.markUnknown("TIMEOUT", "request timed out");
        XuiClientProvision active = provision();
        active.markProvisioning();
        active.markActive(NOW);
        when(inboundClient.getInboundById(7L)).thenReturn(Optional.of(inbound(7)));
        when(prepareTransaction.prepare(123L, SELECTION_ID, 7)).thenReturn(new PreparedXuiProvision(pending, true));
        when(statusTransaction.claimProvisioning(PROVISION_ID)).thenReturn(new ClaimedXuiProvision(provisioning, true));
        when(managementClient.createClient(any(CreateXuiClientRequest.class)))
                .thenThrow(new XuiClientCreateTimeoutException("timeout", new RuntimeException("timeout")));
        when(statusTransaction.markUnknown(PROVISION_ID, "XuiClientCreateTimeoutException", "timeout"))
                .thenReturn(unknown);
        when(inboundClient.findClient(7, CLIENT_ID, "vpn_user_provision"))
                .thenReturn(Optional.of(new XuiClientSnapshot(CLIENT_ID, "vpn_user_provision", true, 0, 0, 0, null, 0, "sub123", "xtls-rprx-vision")));
        when(statusTransaction.markActive(PROVISION_ID, NOW)).thenReturn(active);

        CreateVpnClientResult result = service.create(new CreateVpnClientCommand(123L, SELECTION_ID, 7L));

        assertThat(result.status()).isEqualTo(XuiProvisionStatus.ACTIVE);
        verify(managementClient).createClient(any(CreateXuiClientRequest.class));
    }

    @Test
    void confirmedRemoteRejectionMarksFailed() {
        XuiClientProvision pending = provision();
        XuiClientProvision provisioning = provision();
        provisioning.markProvisioning();
        XuiClientProvision failed = provision();
        failed.markFailed("REMOTE_REJECTED", "duplicate client");
        when(inboundClient.getInboundById(7L)).thenReturn(Optional.of(inbound(7)));
        when(prepareTransaction.prepare(123L, SELECTION_ID, 7)).thenReturn(new PreparedXuiProvision(pending, true));
        when(statusTransaction.claimProvisioning(PROVISION_ID)).thenReturn(new ClaimedXuiProvision(provisioning, true));
        when(managementClient.createClient(any(CreateXuiClientRequest.class)))
                .thenThrow(new XuiClientCreateRejectedException("duplicate client", new RuntimeException("duplicate")));
        when(statusTransaction.markFailed(PROVISION_ID, "REMOTE_REJECTED", "duplicate client")).thenReturn(failed);

        assertThatThrownBy(() -> service.create(new CreateVpnClientCommand(123L, SELECTION_ID, 7L)))
                .isInstanceOf(XuiClientProvisionFailedException.class);
    }

    private static XuiClientProvision provision() {
        XuiClientProvision provision = XuiClientProvision.createPending(
                USER_ID,
                PLAN_ID,
                SELECTION_ID,
                7,
                CLIENT_ID,
                "vpn_user_provision",
                "sub123",
                1024,
                NOW.plusSeconds(86_400),
                1,
                NOW
        );
        ReflectionTestUtils.setField(provision, "id", PROVISION_ID);
        return provision;
    }

    private static XuiInboundSnapshot inbound(long id) {
        return new XuiInboundSnapshot(
                id,
                "Reality",
                "VLESS",
                443,
                true,
                null,
                0,
                0,
                0,
                null,
                List.of(),
                "tcp",
                "REALITY",
                "vpn.example.test",
                "PUBLIC_KEY",
                "abcd"
        );
    }
}
