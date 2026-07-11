package com.parazit.panel.application.xui.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.xui.XuiClientManagementClient;
import com.parazit.panel.application.xui.client.command.DisableVpnClientCommand;
import com.parazit.panel.application.xui.client.model.DisableXuiClientRequest;
import com.parazit.panel.application.xui.client.model.DisableXuiClientResponse;
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

class DisableVpnClientServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");
    private static final UUID PROVISION_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private XuiClientLifecycleTransaction transaction;
    private XuiRemoteClientLookupService lookupService;
    private XuiClientManagementClient managementClient;
    private DisableVpnClientService service;

    @BeforeEach
    void setUp() {
        transaction = org.mockito.Mockito.mock(XuiClientLifecycleTransaction.class);
        lookupService = org.mockito.Mockito.mock(XuiRemoteClientLookupService.class);
        managementClient = org.mockito.Mockito.mock(XuiClientManagementClient.class);
        SystemClockPort clock = () -> NOW;
        service = new DisableVpnClientService(
                transaction,
                lookupService,
                managementClient,
                clock,
                new XuiClientProvisioningProperties("xtls-rprx-vision", 1),
                new XuiClientLifecycleResultMapper()
        );
    }

    @Test
    void disablesActiveProvisionAndMarksDisabled() {
        XuiClientProvision disabling = activeProvision();
        disabling.markDisabling();
        XuiClientProvision disabled = activeProvision();
        disabled.markDisabling();
        disabled.markDisabled(NOW);
        when(transaction.prepareDisable(123L, PROVISION_ID))
                .thenReturn(new PreparedXuiClientLifecycleOperation(disabling, true, false));
        when(lookupService.findVerified(disabling))
                .thenReturn(Optional.of(snapshot(true)))
                .thenReturn(Optional.of(snapshot(false)));
        when(managementClient.disableClient(any(DisableXuiClientRequest.class)))
                .thenReturn(new DisableXuiClientResponse(7, snapshot(true).clientId(), true, false, "ok"));
        when(transaction.find(PROVISION_ID)).thenReturn(disabling);
        when(transaction.markDisabled(PROVISION_ID, NOW)).thenReturn(disabled);

        XuiClientLifecycleResult result = service.disable(new DisableVpnClientCommand(123L, PROVISION_ID));

        assertThat(result.status()).isEqualTo(XuiProvisionStatus.DISABLED);
        assertThat(result.changed()).isTrue();
        verify(managementClient).disableClient(any(DisableXuiClientRequest.class));
    }

    @Test
    void alreadyDisabledReturnsIdempotentlyWithoutRemoteCall() {
        XuiClientProvision disabled = activeProvision();
        disabled.markDisabling();
        disabled.markDisabled(NOW);
        when(transaction.prepareDisable(123L, PROVISION_ID))
                .thenReturn(new PreparedXuiClientLifecycleOperation(disabled, false, true));

        XuiClientLifecycleResult result = service.disable(new DisableVpnClientCommand(123L, PROVISION_ID));

        assertThat(result.changed()).isFalse();
        assertThat(result.status()).isEqualTo(XuiProvisionStatus.DISABLED);
        verify(managementClient, never()).disableClient(any());
    }

    private static XuiClientProvision activeProvision() {
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
        provision.markActive(NOW.minusSeconds(60));
        return provision;
    }

    private static XuiClientSnapshot snapshot(boolean enabled) {
        return new XuiClientSnapshot(
                "11111111-1111-1111-1111-111111111111",
                "vpn_abc_def",
                enabled,
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
