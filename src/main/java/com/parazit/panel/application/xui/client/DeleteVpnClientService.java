package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.port.in.xui.client.DeleteVpnClientUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.xui.XuiClientManagementClient;
import com.parazit.panel.application.xui.client.command.DeleteVpnClientCommand;
import com.parazit.panel.application.xui.client.model.DeleteXuiClientRequest;
import com.parazit.panel.application.xui.client.result.XuiClientLifecycleResult;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.config.properties.XuiClientProvisioningProperties;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DeleteVpnClientService implements DeleteVpnClientUseCase {

    private final XuiClientLifecycleTransaction transaction;
    private final XuiRemoteClientLookupService lookupService;
    private final XuiClientManagementClient managementClient;
    private final SystemClockPort clock;
    private final XuiClientProvisioningProperties properties;
    private final XuiClientLifecycleResultMapper mapper;

    public DeleteVpnClientService(
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
    public XuiClientLifecycleResult delete(DeleteVpnClientCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        PreparedXuiClientLifecycleOperation prepared = transaction.prepareDelete(
                command.telegramUserId(),
                command.provisionId(),
                command.force()
        );
        XuiClientProvision provision = prepared.provision();
        if (prepared.idempotent()) {
            return mapper.toResult(provision, false, false);
        }
        if (!prepared.claimed()) {
            return reconcileDelete(provision, false, 0);
        }
        return deleteRemote(provision, true, properties.reconciliationAttempts());
    }

    private XuiClientLifecycleResult deleteRemote(XuiClientProvision provision, boolean changed, int remainingAttempts) {
        Optional<XuiClientSnapshot> remote = lookupService.findVerified(provision);
        if (remote.isEmpty()) {
            return mapper.toResult(transaction.markDeleted(provision.getId(), clock.now()), changed, false);
        }
        try {
            managementClient.deleteClient(new DeleteXuiClientRequest(
                    provision.getInboundId(),
                    provision.getRemoteClientId(),
                    provision.getRemoteEmail()
            ));
            return reconcileDelete(transaction.find(provision.getId()), changed, remainingAttempts);
        } catch (XuiClientRemoteOperationTimeoutException | XuiClientRemoteOperationConnectionException exception) {
            XuiClientProvision unknown = transaction.markUnknown(
                    provision.getId(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
            return reconcileDelete(unknown, changed, remainingAttempts);
        } catch (XuiClientRemoteOperationRejectedException exception) {
            XuiClientProvision failed = transaction.markFailed(
                    provision.getId(),
                    "REMOTE_DELETE_REJECTED",
                    exception.getMessage()
            );
            throw new XuiClientDeleteFailedException(failed.getFailureMessage());
        }
    }

    private XuiClientLifecycleResult reconcileDelete(XuiClientProvision provision, boolean changed, int remainingAttempts) {
        Optional<XuiClientSnapshot> remote = lookupService.findVerified(provision);
        if (remote.isEmpty()) {
            return mapper.toResult(transaction.markDeleted(provision.getId(), clock.now()), changed, false);
        }
        if (remainingAttempts > 0) {
            return deleteRemote(transaction.find(provision.getId()), changed, remainingAttempts - 1);
        }
        throw new XuiClientOperationUnknownException("Xui client delete result is unknown");
    }
}
