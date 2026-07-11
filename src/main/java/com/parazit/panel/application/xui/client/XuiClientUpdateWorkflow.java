package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.xui.XuiClientManagementClient;
import com.parazit.panel.application.port.out.xui.XuiClientStateClient;
import com.parazit.panel.application.xui.client.model.ResetXuiClientTrafficRequest;
import com.parazit.panel.application.xui.client.model.RenewalMode;
import com.parazit.panel.application.xui.client.model.UpdateXuiClientRequest;
import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.config.properties.XuiClientProvisioningProperties;
import com.parazit.panel.domain.xui.operation.XuiClientOperation;
import com.parazit.panel.domain.xui.operation.XuiClientOperationType;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class XuiClientUpdateWorkflow {

    private final XuiClientOperationTransaction transaction;
    private final XuiClientStateClient stateClient;
    private final XuiClientManagementClient managementClient;
    private final SystemClockPort clock;
    private final XuiClientProvisioningProperties properties;
    private final XuiClientUpdateResultMapper resultMapper;

    public XuiClientUpdateWorkflow(
            XuiClientOperationTransaction transaction,
            XuiClientStateClient stateClient,
            XuiClientManagementClient managementClient,
            SystemClockPort clock,
            XuiClientProvisioningProperties properties,
            XuiClientUpdateResultMapper resultMapper
    ) {
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.stateClient = Objects.requireNonNull(stateClient, "stateClient must not be null");
        this.managementClient = Objects.requireNonNull(managementClient, "managementClient must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.resultMapper = Objects.requireNonNull(resultMapper, "resultMapper must not be null");
    }

    public XuiClientUpdateResult renew(
            UUID operationId,
            Long telegramUserId,
            UUID provisionId,
            int durationDays,
            RenewalMode mode
    ) {
        if (durationDays <= 0) {
            throw new IllegalArgumentException("durationDays must be positive");
        }
        RenewalMode renewalMode = Objects.requireNonNull(mode, "renewalMode must not be null");
        return executeUpdate(
                operationId,
                telegramUserId,
                provisionId,
                XuiClientOperationType.RENEW_EXPIRY,
                Map.of("durationDays", durationDays, "renewalMode", renewalMode),
                (provision, remote, now) -> {
                    Instant current = remote.expiryTime() == null ? provision.getExpiresAt() : remote.expiryTime();
                    Instant base = renewalMode == RenewalMode.EXTEND_FROM_NOW || current.isBefore(now)
                            ? now
                            : current;
                    Instant next = base.plus(durationDays, ChronoUnit.DAYS);
                    if (!next.isAfter(current)) {
                        return UpdatePlan.noop();
                    }
                    return new UpdatePlan(new UpdateXuiClientRequest(
                            provision.getInboundId(),
                            provision.getRemoteClientId(),
                            provision.getRemoteEmail(),
                            null,
                            next,
                            null,
                            null,
                            null
                    ), next, null, null, null);
                }
        );
    }

    public XuiClientUpdateResult replaceTrafficLimit(
            UUID operationId,
            Long telegramUserId,
            UUID provisionId,
            long trafficLimitBytes
    ) {
        if (trafficLimitBytes < 0) {
            throw new IllegalArgumentException("trafficLimitBytes must be zero or positive");
        }
        return executeUpdate(
                operationId,
                telegramUserId,
                provisionId,
                XuiClientOperationType.REPLACE_TRAFFIC_LIMIT,
                Map.of("trafficLimitBytes", trafficLimitBytes),
                (provision, remote, now) -> {
                    if (remote.totalTrafficLimitBytes() == trafficLimitBytes) {
                        return UpdatePlan.noop();
                    }
                    return new UpdatePlan(new UpdateXuiClientRequest(
                            provision.getInboundId(),
                            provision.getRemoteClientId(),
                            provision.getRemoteEmail(),
                            null,
                            null,
                            trafficLimitBytes,
                            null,
                            null
                    ), null, trafficLimitBytes, null, null);
                }
        );
    }

    public XuiClientUpdateResult addTraffic(
            UUID operationId,
            Long telegramUserId,
            UUID provisionId,
            long additionalTrafficBytes
    ) {
        if (additionalTrafficBytes <= 0) {
            throw new IllegalArgumentException("additionalTrafficBytes must be positive");
        }
        return executeUpdate(
                operationId,
                telegramUserId,
                provisionId,
                XuiClientOperationType.ADD_TRAFFIC,
                Map.of("additionalTrafficBytes", additionalTrafficBytes),
                (provision, remote, now) -> {
                    if (remote.totalTrafficLimitBytes() == 0) {
                        throw new XuiClientOperationNotAllowedException("Cannot add traffic to an unlimited Xui client");
                    }
                    long next;
                    try {
                        next = Math.addExact(remote.totalTrafficLimitBytes(), additionalTrafficBytes);
                    } catch (ArithmeticException exception) {
                        throw new XuiTrafficOverflowException();
                    }
                    return new UpdatePlan(new UpdateXuiClientRequest(
                            provision.getInboundId(),
                            provision.getRemoteClientId(),
                            provision.getRemoteEmail(),
                            null,
                            null,
                            next,
                            null,
                            null
                    ), null, next, null, null);
                }
        );
    }

    public XuiClientUpdateResult enable(UUID operationId, Long telegramUserId, UUID provisionId) {
        return executeUpdate(
                operationId,
                telegramUserId,
                provisionId,
                XuiClientOperationType.ENABLE,
                Map.of("enabled", true),
                (provision, remote, now) -> {
                    if (remote.enabled() && provision.getStatus() == XuiProvisionStatus.ACTIVE) {
                        return UpdatePlan.noop();
                    }
                    if (remote.expiryTime() != null && !remote.expiryTime().isAfter(now)) {
                        throw new XuiClientOperationNotAllowedException("Expired Xui client must be renewed before enable");
                    }
                    return new UpdatePlan(new UpdateXuiClientRequest(
                            provision.getInboundId(),
                            provision.getRemoteClientId(),
                            provision.getRemoteEmail(),
                            true,
                            null,
                            null,
                            null,
                            null
                    ), null, null, null, true);
                }
        );
    }

    public XuiClientUpdateResult changeIpLimit(
            UUID operationId,
            Long telegramUserId,
            UUID provisionId,
            int ipLimit
    ) {
        if (ipLimit < 0) {
            throw new IllegalArgumentException("ipLimit must be zero or positive");
        }
        return executeUpdate(
                operationId,
                telegramUserId,
                provisionId,
                XuiClientOperationType.CHANGE_IP_LIMIT,
                Map.of("ipLimit", ipLimit),
                (provision, remote, now) -> {
                    if (remote.ipLimit() == ipLimit) {
                        return UpdatePlan.noop();
                    }
                    return new UpdatePlan(new UpdateXuiClientRequest(
                            provision.getInboundId(),
                            provision.getRemoteClientId(),
                            provision.getRemoteEmail(),
                            null,
                            null,
                            null,
                            ipLimit,
                            null
                    ), null, null, ipLimit, null);
                }
        );
    }

    public XuiClientUpdateResult resetTraffic(UUID operationId, Long telegramUserId, UUID provisionId) {
        Instant now = clock.now();
        PreparedXuiClientUpdateOperation prepared = prepare(
                operationId,
                telegramUserId,
                provisionId,
                XuiClientOperationType.RESET_TRAFFIC,
                Map.of("resetTraffic", true),
                now
        );
        if (prepared.replay()) {
            return resultMapper.toResult(prepared.provision(), prepared.operation(), findRemote(prepared.provision()).orElse(null), false, now);
        }
        XuiClientSnapshot remote = verifiedRemote(prepared.provision());
        if (remote.uploadBytes() == 0 && remote.downloadBytes() == 0) {
            XuiClientProvision saved = transaction.markSucceeded(
                    prepared.provision().getId(),
                    prepared.operation().getOperationId(),
                    now,
                    provision -> provision.synchronizeRemoteState(
                            remote.enabled(),
                            remote.totalTrafficLimitBytes(),
                            remote.expiryTime() == null ? provision.getExpiresAt() : remote.expiryTime(),
                            remote.ipLimit(),
                            0,
                            0,
                            now
                    )
            );
            return resultMapper.toResult(saved, transaction.findOperation(operationId), remote, false, now);
        }
        try {
            managementClient.resetTraffic(new ResetXuiClientTrafficRequest(
                    prepared.provision().getInboundId(),
                    prepared.provision().getRemoteClientId(),
                    prepared.provision().getRemoteEmail()
            ));
            return completeReset(prepared, now, true);
        } catch (XuiClientRemoteOperationTimeoutException | XuiClientRemoteOperationConnectionException exception) {
            Optional<XuiClientSnapshot> reconciled = findRemote(prepared.provision());
            if (reconciled.isPresent() && reconciled.get().uploadBytes() == 0 && reconciled.get().downloadBytes() == 0) {
                return completeReset(prepared, now, true);
            }
            transaction.markUnknown(prepared.provision().getId(), operationId, "REMOTE_UNCERTAIN", "Xui traffic reset result is unknown");
            throw new XuiClientTrafficResetUnknownException("Xui traffic reset result is unknown");
        } catch (XuiClientRemoteOperationRejectedException exception) {
            transaction.markFailed(prepared.provision().getId(), operationId, "REMOTE_REJECTED", exception.getMessage(), now);
            throw new XuiClientTrafficResetFailedException("Xui traffic reset failed");
        }
    }

    public XuiClientUpdateResult synchronize(UUID operationId, Long telegramUserId, UUID provisionId) {
        Instant now = clock.now();
        PreparedXuiClientUpdateOperation prepared = prepare(
                operationId,
                telegramUserId,
                provisionId,
                XuiClientOperationType.SYNCHRONIZE,
                Map.of("synchronize", true),
                now
        );
        if (prepared.replay()) {
            return resultMapper.toResult(prepared.provision(), prepared.operation(), findRemote(prepared.provision()).orElse(null), false, now);
        }
        XuiClientSnapshot remote = findRemote(prepared.provision())
                .orElseThrow(XuiRemoteClientMissingException::new);
        verifyIdentity(prepared.provision(), remote);
        XuiClientProvision saved = transaction.markSucceeded(
                prepared.provision().getId(),
                operationId,
                now,
                provision -> provision.synchronizeRemoteState(
                        remote.enabled(),
                        remote.totalTrafficLimitBytes(),
                        remote.expiryTime() == null ? provision.getExpiresAt() : remote.expiryTime(),
                        remote.ipLimit(),
                        remote.uploadBytes(),
                        remote.downloadBytes(),
                        now
                )
        );
        return resultMapper.toResult(saved, transaction.findOperation(operationId), remote, true, now);
    }

    private XuiClientUpdateResult executeUpdate(
            UUID operationId,
            Long telegramUserId,
            UUID provisionId,
            XuiClientOperationType type,
            Map<String, ?> fingerprintValues,
            UpdatePlanner planner
    ) {
        Instant now = clock.now();
        PreparedXuiClientUpdateOperation prepared = prepare(operationId, telegramUserId, provisionId, type, fingerprintValues, now);
        if (prepared.replay()) {
            return resultMapper.toResult(prepared.provision(), prepared.operation(), findRemote(prepared.provision()).orElse(null), false, now);
        }
        XuiClientSnapshot remote = verifiedRemote(prepared.provision());
        UpdatePlan plan = planner.plan(prepared.provision(), remote, now);
        if (plan.isNoop()) {
            XuiClientProvision saved = transaction.markSucceeded(
                    prepared.provision().getId(),
                    operationId,
                    now,
                    provision -> provision.synchronizeRemoteState(
                            remote.enabled(),
                            remote.totalTrafficLimitBytes(),
                            remote.expiryTime() == null ? provision.getExpiresAt() : remote.expiryTime(),
                            remote.ipLimit(),
                            remote.uploadBytes(),
                            remote.downloadBytes(),
                            now
                    )
            );
            return resultMapper.toResult(saved, transaction.findOperation(operationId), remote, false, now);
        }
        try {
            managementClient.updateClient(plan.request());
            return completeUpdate(prepared, plan, now, true);
        } catch (XuiClientRemoteOperationTimeoutException | XuiClientRemoteOperationConnectionException exception) {
            Optional<XuiClientSnapshot> reconciled = findRemote(prepared.provision());
            if (reconciled.isPresent() && plan.matches(reconciled.get())) {
                return completeUpdate(prepared, plan, now, true);
            }
            transaction.markUnknown(prepared.provision().getId(), operationId, "REMOTE_UNCERTAIN", "Xui client update result is unknown");
            throw new XuiClientUpdateUnknownException("Xui client update result is unknown");
        } catch (XuiClientRemoteOperationRejectedException exception) {
            transaction.markFailed(prepared.provision().getId(), operationId, "REMOTE_REJECTED", exception.getMessage(), now);
            throw new XuiClientUpdateFailedException("Xui client update failed");
        }
    }

    private XuiClientUpdateResult completeUpdate(
            PreparedXuiClientUpdateOperation prepared,
            UpdatePlan plan,
            Instant now,
            boolean changed
    ) {
        XuiClientSnapshot verified = findRemote(prepared.provision())
                .orElseThrow(XuiRemoteClientMissingException::new);
        verifyIdentity(prepared.provision(), verified);
        if (!plan.matches(verified)) {
            transaction.markUnknown(prepared.provision().getId(), prepared.operation().getOperationId(), "REMOTE_MISMATCH", "Xui remote state did not match requested update");
            throw new XuiClientUpdateUnknownException("Xui remote state did not match requested update");
        }
        XuiClientProvision saved = transaction.markSucceeded(
                prepared.provision().getId(),
                prepared.operation().getOperationId(),
                now,
                provision -> provision.synchronizeRemoteState(
                        verified.enabled(),
                        verified.totalTrafficLimitBytes(),
                        verified.expiryTime() == null ? provision.getExpiresAt() : verified.expiryTime(),
                        verified.ipLimit(),
                        verified.uploadBytes(),
                        verified.downloadBytes(),
                        now
                )
        );
        return resultMapper.toResult(saved, transaction.findOperation(prepared.operation().getOperationId()), verified, changed, now);
    }

    private XuiClientUpdateResult completeReset(
            PreparedXuiClientUpdateOperation prepared,
            Instant now,
            boolean changed
    ) {
        XuiClientSnapshot verified = findRemote(prepared.provision())
                .orElseThrow(XuiRemoteClientMissingException::new);
        verifyIdentity(prepared.provision(), verified);
        if (verified.uploadBytes() != 0 || verified.downloadBytes() != 0) {
            transaction.markUnknown(prepared.provision().getId(), prepared.operation().getOperationId(), "REMOTE_MISMATCH", "Xui traffic was not reset");
            throw new XuiClientTrafficResetUnknownException("Xui traffic was not reset");
        }
        XuiClientProvision saved = transaction.markSucceeded(
                prepared.provision().getId(),
                prepared.operation().getOperationId(),
                now,
                provision -> provision.synchronizeRemoteState(
                        verified.enabled(),
                        verified.totalTrafficLimitBytes(),
                        verified.expiryTime() == null ? provision.getExpiresAt() : verified.expiryTime(),
                        verified.ipLimit(),
                        0,
                        0,
                        now
                )
        );
        return resultMapper.toResult(saved, transaction.findOperation(prepared.operation().getOperationId()), verified, changed, now);
    }

    private PreparedXuiClientUpdateOperation prepare(
            UUID operationId,
            Long telegramUserId,
            UUID provisionId,
            XuiClientOperationType type,
            Map<String, ?> fingerprintValues,
            Instant now
    ) {
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(telegramUserId, "telegramUserId must not be null");
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        Objects.requireNonNull(provisionId, "provisionId must not be null");
        String fingerprint = XuiClientOperationFingerprint.of(provisionId, type, fingerprintValues);
        PreparedXuiClientUpdateOperation prepared = transaction.prepare(
                telegramUserId,
                provisionId,
                operationId,
                type,
                fingerprint,
                now
        );
        return prepared;
    }

    private XuiClientSnapshot verifiedRemote(XuiClientProvision provision) {
        XuiClientSnapshot remote = findRemote(provision).orElseThrow(XuiRemoteClientMissingException::new);
        verifyIdentity(provision, remote);
        return remote;
    }

    private Optional<XuiClientSnapshot> findRemote(XuiClientProvision provision) {
        return stateClient.findClient(provision.getInboundId(), provision.getRemoteClientId());
    }

    private static void verifyIdentity(XuiClientProvision provision, XuiClientSnapshot remote) {
        if (remote.clientId() == null || !remote.clientId().equalsIgnoreCase(provision.getRemoteClientId())) {
            throw new XuiRemoteClientIdentityMismatchException();
        }
        if (remote.email() != null
                && !remote.email().isBlank()
                && !remote.email().equalsIgnoreCase(provision.getRemoteEmail())) {
            throw new XuiRemoteClientIdentityMismatchException();
        }
    }

    private record UpdatePlan(
            UpdateXuiClientRequest request,
            Instant targetExpiry,
            Long targetTrafficLimit,
            Integer targetIpLimit,
            Boolean targetEnabled
    ) {

        static UpdatePlan noop() {
            return new UpdatePlan(null, null, null, null, null);
        }

        boolean isNoop() {
            return request == null;
        }

        boolean matches(XuiClientSnapshot remote) {
            if (targetExpiry != null && (remote.expiryTime() == null || !remote.expiryTime().equals(targetExpiry))) {
                return false;
            }
            if (targetTrafficLimit != null && remote.totalTrafficLimitBytes() != targetTrafficLimit) {
                return false;
            }
            if (targetIpLimit != null && remote.ipLimit() != targetIpLimit) {
                return false;
            }
            return targetEnabled == null || remote.enabled() == targetEnabled;
        }
    }

    @FunctionalInterface
    private interface UpdatePlanner {

        UpdatePlan plan(XuiClientProvision provision, XuiClientSnapshot remote, Instant now);
    }
}
