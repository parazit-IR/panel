package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.port.in.xui.client.CreateVpnClientUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.xui.XuiClientManagementClient;
import com.parazit.panel.application.port.out.xui.XuiInboundClient;
import com.parazit.panel.application.xui.client.command.CreateVpnClientCommand;
import com.parazit.panel.application.xui.client.model.CreateXuiClientRequest;
import com.parazit.panel.application.xui.client.result.CreateVpnClientResult;
import com.parazit.panel.application.xui.inbound.XuiInboundEligibilityPolicy;
import com.parazit.panel.application.xui.inbound.XuiInboundNotFoundException;
import com.parazit.panel.application.xui.model.XuiInboundSnapshot;
import com.parazit.panel.config.properties.XuiClientProvisioningProperties;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CreateVpnClientService implements CreateVpnClientUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateVpnClientService.class);

    private final PrepareXuiProvisionTransaction prepareTransaction;
    private final UpdateXuiProvisionStatusTransaction statusTransaction;
    private final XuiInboundClient inboundClient;
    private final XuiInboundEligibilityPolicy eligibilityPolicy;
    private final XuiClientManagementClient managementClient;
    private final SystemClockPort clock;
    private final XuiClientProvisioningProperties properties;
    private final CreateVpnClientResultMapper mapper;

    public CreateVpnClientService(
            PrepareXuiProvisionTransaction prepareTransaction,
            UpdateXuiProvisionStatusTransaction statusTransaction,
            XuiInboundClient inboundClient,
            XuiInboundEligibilityPolicy eligibilityPolicy,
            XuiClientManagementClient managementClient,
            SystemClockPort clock,
            XuiClientProvisioningProperties properties,
            CreateVpnClientResultMapper mapper
    ) {
        this.prepareTransaction = Objects.requireNonNull(prepareTransaction, "prepareTransaction must not be null");
        this.statusTransaction = Objects.requireNonNull(statusTransaction, "statusTransaction must not be null");
        this.inboundClient = Objects.requireNonNull(inboundClient, "inboundClient must not be null");
        this.eligibilityPolicy = Objects.requireNonNull(eligibilityPolicy, "eligibilityPolicy must not be null");
        this.managementClient = Objects.requireNonNull(managementClient, "managementClient must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public CreateVpnClientResult create(CreateVpnClientCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        XuiInboundSnapshot inbound = resolveInbound(command.inboundId());
        PreparedXuiProvision prepared = prepareTransaction.prepare(
                command.telegramUserId(),
                command.planSelectionId(),
                inbound.id()
        );
        XuiClientProvision provision = prepared.provision();
        if (provision.getStatus() == XuiProvisionStatus.ACTIVE) {
            log.atInfo().addKeyValue("provisionId", provision.getId()).log("Returned existing active Xui provision");
            return mapper.toResult(provision, false);
        }
        if (!prepared.newlyCreated() && provision.getStatus() == XuiProvisionStatus.PROVISIONING) {
            return reconcileOrUnknown(provision, false);
        }
        if (!prepared.newlyCreated() && provision.getStatus() == XuiProvisionStatus.UNKNOWN && remoteClientExists(provision)) {
            XuiClientProvision active = statusTransaction.markActive(provision.getId(), clock.now());
            return mapper.toResult(active, false);
        }

        ClaimedXuiProvision claimed = statusTransaction.claimProvisioning(provision.getId());
        if (!claimed.claimed() && !prepared.newlyCreated()) {
            return reconcileOrUnknown(claimed.provision(), false);
        }
        return provisionRemoteClient(claimed.provision(), prepared.newlyCreated(), properties.reconciliationAttempts());
    }

    private CreateVpnClientResult provisionRemoteClient(
            XuiClientProvision provision,
            boolean newlyCreated,
            int remainingRetryAttempts
    ) {
        try {
            managementClient.createClient(new CreateXuiClientRequest(
                    provision.getInboundId(),
                    provision.getRemoteClientId(),
                    provision.getRemoteEmail(),
                    provision.getRemoteSubscriptionId(),
                    true,
                    provision.getTrafficLimitBytes(),
                    provision.getExpiresAt(),
                    provision.getIpLimit(),
                    properties.defaultFlow()
            ));
            XuiClientProvision active = statusTransaction.markActive(provision.getId(), clock.now());
            log.atInfo().addKeyValue("provisionId", active.getId()).log("Xui client provisioning confirmed");
            return mapper.toResult(active, newlyCreated);
        } catch (XuiClientCreateTimeoutException | XuiClientCreateConnectionException exception) {
            XuiClientProvision unknown = statusTransaction.markUnknown(
                    provision.getId(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
            return reconcileAfterUncertainResult(unknown, newlyCreated, remainingRetryAttempts);
        } catch (XuiClientCreateRejectedException exception) {
            XuiClientProvision failed = statusTransaction.markFailed(
                    provision.getId(),
                    "REMOTE_REJECTED",
                    exception.getMessage()
            );
            throw new XuiClientProvisionFailedException(failed.getFailureMessage());
        }
    }

    private CreateVpnClientResult reconcileAfterUncertainResult(
            XuiClientProvision provision,
            boolean newlyCreated,
            int remainingRetryAttempts
    ) {
        if (remoteClientExists(provision)) {
            XuiClientProvision active = statusTransaction.markActive(provision.getId(), clock.now());
            return mapper.toResult(active, newlyCreated);
        }
        if (remainingRetryAttempts > 0) {
            return retryAfterConfirmedAbsent(provision, newlyCreated, remainingRetryAttempts - 1);
        }
        throw new XuiClientProvisionUnknownException("Xui client provisioning result is unknown");
    }

    private CreateVpnClientResult retryAfterConfirmedAbsent(
            XuiClientProvision provision,
            boolean newlyCreated,
            int remainingRetryAttempts
    ) {
        ClaimedXuiProvision claimed = statusTransaction.claimProvisioning(provision.getId());
        if (!claimed.claimed()) {
            return reconcileOrUnknown(claimed.provision(), newlyCreated);
        }
        return provisionRemoteClient(claimed.provision(), newlyCreated, remainingRetryAttempts);
    }

    private CreateVpnClientResult reconcileOrUnknown(XuiClientProvision provision, boolean newlyCreated) {
        if (remoteClientExists(provision)) {
            XuiClientProvision active = statusTransaction.markActive(provision.getId(), clock.now());
            return mapper.toResult(active, newlyCreated);
        }
        throw new XuiClientProvisionUnknownException("Xui client provisioning is already in progress or unknown");
    }

    private boolean remoteClientExists(XuiClientProvision provision) {
        return inboundClient.findClient(
                provision.getInboundId(),
                provision.getRemoteClientId(),
                provision.getRemoteEmail()
        ).isPresent();
    }

    private XuiInboundSnapshot resolveInbound(Long inboundId) {
        if (inboundId == null) {
            return inboundClient.getInbounds()
                    .stream()
                    .filter(eligibilityPolicy::isEligible)
                    .min(Comparator.comparingLong(XuiInboundSnapshot::id))
                    .orElseThrow(() -> new XuiInboundNotEligibleException(0));
        }
        XuiInboundSnapshot inbound = inboundClient.getInboundById(inboundId)
                .orElseThrow(() -> new XuiInboundNotFoundException(inboundId));
        if (!eligibilityPolicy.isEligible(inbound)) {
            throw new XuiInboundNotEligibleException(inboundId);
        }
        return inbound;
    }
}
