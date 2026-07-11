package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.port.in.xui.client.DisableVpnClientUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.xui.XuiClientManagementClient;
import com.parazit.panel.application.xui.client.command.DisableVpnClientCommand;
import com.parazit.panel.application.xui.client.model.DisableXuiClientRequest;
import com.parazit.panel.application.xui.client.result.XuiClientLifecycleResult;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.config.properties.XuiClientProvisioningProperties;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DisableVpnClientService implements DisableVpnClientUseCase {

    private final XuiClientLifecycleTransaction transaction;
    private final XuiRemoteClientLookupService lookupService;
    private final XuiClientManagementClient managementClient;
    private final SystemClockPort clock;
    private final XuiClientProvisioningProperties properties;
    private final XuiClientLifecycleResultMapper mapper;

    public DisableVpnClientService(
            XuiClientLifecycleTransaction transaction,
            XuiRemoteClientLookupService lookupService,
            XuiClientManagementClient managementClient,
            SystemClockPort clock,
            XuiClientProvisioningProperties properties,
            XuiClientLifecycleResultMapper mapper
    ) {
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.lookupService = Objects.requireNonNull(lookupService, "lookupService must not be null");
        this.managementClient = Objects.requireNonNull(managementClient, "managementClient must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public XuiClientLifecycleResult disable(DisableVpnClientCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        PreparedXuiClientLifecycleOperation prepared = transaction.prepareDisable(command.telegramUserId(), command.provisionId());
        XuiClientProvision provision = prepared.provision();
        if (prepared.idempotent()) {
            return mapper.toResult(provision, false, true);
        }
        if (!prepared.claimed()) {
            return reconcileDisable(provision, false, 0);
        }
        return disableRemote(provision, true, properties.reconciliationAttempts());
    }

    private XuiClientLifecycleResult disableRemote(XuiClientProvision provision, boolean changed, int remainingAttempts) {
        Optional<XuiClientSnapshot> remote = lookupService.findVerified(provision);
        if (remote.isEmpty()) {
            XuiClientProvision unknown = transaction.markUnknown(
                    provision.getId(),
                    "REMOTE_CLIENT_ABSENT",
                    "Remote Xui client was not found during disable"
            );
            throw new XuiClientOperationUnknownException(unknown.getFailureMessage());
        }
        if (!remote.orElseThrow().enabled()) {
            return mapper.toResult(transaction.markDisabled(provision.getId(), clock.now()), changed, true);
        }
        try {
            managementClient.disableClient(new DisableXuiClientRequest(
                    provision.getInboundId(),
                    provision.getRemoteClientId(),
                    provision.getRemoteEmail()
            ));
            return reconcileDisable(transaction.find(provision.getId()), changed, remainingAttempts);
        } catch (XuiClientRemoteOperationTimeoutException | XuiClientRemoteOperationConnectionException exception) {
            XuiClientProvision unknown = transaction.markUnknown(
                    provision.getId(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
            return reconcileDisable(unknown, changed, remainingAttempts);
        } catch (XuiClientRemoteOperationRejectedException exception) {
            XuiClientProvision failed = transaction.markFailed(
                    provision.getId(),
                    "REMOTE_DISABLE_REJECTED",
                    exception.getMessage()
            );
            throw new XuiClientDisableFailedException(failed.getFailureMessage());
        }
    }

    private XuiClientLifecycleResult reconcileDisable(XuiClientProvision provision, boolean changed, int remainingAttempts) {
        Optional<XuiClientSnapshot> remote = lookupService.findVerified(provision);
        if (remote.isPresent() && !remote.orElseThrow().enabled()) {
            return mapper.toResult(transaction.markDisabled(provision.getId(), clock.now()), changed, true);
        }
        if (remote.isPresent() && remainingAttempts > 0) {
            return disableRemote(transaction.find(provision.getId()), changed, remainingAttempts - 1);
        }
        throw new XuiClientOperationUnknownException("Xui client disable result is unknown");
    }
}
