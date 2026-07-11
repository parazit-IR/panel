package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.xui.operation.XuiClientOperation;
import com.parazit.panel.domain.xui.operation.XuiClientOperationStatus;
import com.parazit.panel.domain.xui.operation.XuiClientOperationType;
import com.parazit.panel.domain.xui.operation.repository.XuiClientOperationRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class XuiClientOperationTransaction {

    private final UserRepository userRepository;
    private final XuiClientProvisionRepository provisionRepository;
    private final XuiClientOperationRepository operationRepository;

    public XuiClientOperationTransaction(
            UserRepository userRepository,
            XuiClientProvisionRepository provisionRepository,
            XuiClientOperationRepository operationRepository
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.provisionRepository = Objects.requireNonNull(provisionRepository, "provisionRepository must not be null");
        this.operationRepository = Objects.requireNonNull(operationRepository, "operationRepository must not be null");
    }

    @Transactional
    public PreparedXuiClientUpdateOperation prepare(
            Long telegramUserId,
            UUID provisionId,
            UUID operationId,
            XuiClientOperationType type,
            String fingerprint,
            Instant requestedAt
    ) {
        XuiClientProvision provision = verifiedProvision(telegramUserId, provisionId);
        assertAllowedProvisionState(provision, type);

        return operationRepository.findByOperationId(operationId)
                .map(existing -> prepareExisting(provision, existing, type, fingerprint))
                .orElseGet(() -> prepareNew(provision, operationId, type, fingerprint, requestedAt));
    }

    @Transactional
    public XuiClientProvision markSucceeded(
            UUID provisionId,
            UUID operationId,
            Instant completedAt,
            Consumer<XuiClientProvision> provisionMutation
    ) {
        XuiClientProvision provision = findProvision(provisionId);
        provisionMutation.accept(provision);
        XuiClientOperation operation = findOperation(operationId);
        operation.markSucceeded(completedAt);
        operationRepository.save(operation);
        return provisionRepository.save(provision);
    }

    @Transactional
    public XuiClientProvision markUnknown(UUID provisionId, UUID operationId, String code, String message) {
        XuiClientProvision provision = findProvision(provisionId);
        provision.markOperationUnknown(code, message);
        XuiClientOperation operation = findOperation(operationId);
        operation.markUnknown(code, message);
        operationRepository.save(operation);
        return provisionRepository.save(provision);
    }

    @Transactional
    public XuiClientProvision markFailed(UUID provisionId, UUID operationId, String code, String message, Instant completedAt) {
        XuiClientProvision provision = findProvision(provisionId);
        provision.markOperationFailed(code, message);
        XuiClientOperation operation = findOperation(operationId);
        operation.markFailed(code, message, completedAt);
        operationRepository.save(operation);
        return provisionRepository.save(provision);
    }

    @Transactional(readOnly = true)
    public XuiClientProvision findProvision(UUID provisionId) {
        return provisionRepository.findById(provisionId)
                .orElseThrow(() -> new XuiClientProvisionNotFoundException(provisionId));
    }

    @Transactional(readOnly = true)
    public XuiClientOperation findOperation(UUID operationId) {
        return operationRepository.findByOperationId(operationId)
                .orElseThrow(() -> new XuiClientOperationNotFoundException(operationId));
    }

    private PreparedXuiClientUpdateOperation prepareExisting(
            XuiClientProvision provision,
            XuiClientOperation operation,
            XuiClientOperationType type,
            String fingerprint
    ) {
        if (operation.getType() != type || !operation.getRequestFingerprint().equals(fingerprint)) {
            throw new XuiOperationIdConflictException();
        }
        if (operation.getStatus() == XuiClientOperationStatus.IN_PROGRESS) {
            throw new XuiClientOperationInProgressException();
        }
        if (operation.getStatus() == XuiClientOperationStatus.FAILED) {
            throw new XuiClientOperationNotAllowedException("Xui client operation already failed; use a new operation id");
        }
        if (operation.getStatus() == XuiClientOperationStatus.SUCCEEDED) {
            return new PreparedXuiClientUpdateOperation(provision, operation, false, true);
        }
        operation.markInProgress();
        return new PreparedXuiClientUpdateOperation(provision, operationRepository.save(operation), true, false);
    }

    private PreparedXuiClientUpdateOperation prepareNew(
            XuiClientProvision provision,
            UUID operationId,
            XuiClientOperationType type,
            String fingerprint,
            Instant requestedAt
    ) {
        if (operationRepository.existsByProvisionIdAndStatus(provision.getId(), XuiClientOperationStatus.IN_PROGRESS)) {
            throw new XuiClientOperationInProgressException();
        }
        XuiClientOperation operation = XuiClientOperation.create(operationId, provision.getId(), type, fingerprint, requestedAt);
        operation.markInProgress();
        return new PreparedXuiClientUpdateOperation(provision, operationRepository.save(operation), true, false);
    }

    private XuiClientProvision verifiedProvision(Long telegramUserId, UUID provisionId) {
        User user = userRepository.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new UserNotFoundException(telegramUserId));
        XuiClientProvision provision = findProvision(provisionId);
        if (!provision.getUserId().equals(user.getId())) {
            throw new XuiProvisionOwnershipException();
        }
        return provision;
    }

    private static void assertAllowedProvisionState(XuiClientProvision provision, XuiClientOperationType type) {
        XuiProvisionStatus status = provision.getStatus();
        if (status == XuiProvisionStatus.DELETED || status == XuiProvisionStatus.DELETING) {
            throw new XuiClientOperationNotAllowedException("Deleted Xui client provision cannot be updated");
        }
        if (status == XuiProvisionStatus.PROVISIONING
                || status == XuiProvisionStatus.PENDING
                || status == XuiProvisionStatus.DISABLING) {
            throw new XuiClientOperationNotAllowedException("Xui client provision is not ready for updates");
        }
        if (type == XuiClientOperationType.ENABLE && status == XuiProvisionStatus.DELETED) {
            throw new XuiClientOperationNotAllowedException("Deleted Xui client provision cannot be enabled");
        }
    }
}
