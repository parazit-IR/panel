package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.xui.operation.XuiClientOperationStatus;
import com.parazit.panel.domain.xui.operation.repository.XuiClientOperationRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class XuiClientLifecycleTransaction {

    private final UserRepository userRepository;
    private final XuiClientProvisionRepository provisionRepository;
    private final XuiClientOperationRepository operationRepository;

    public XuiClientLifecycleTransaction(
            UserRepository userRepository,
            XuiClientProvisionRepository provisionRepository,
            XuiClientOperationRepository operationRepository
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.provisionRepository = Objects.requireNonNull(provisionRepository, "provisionRepository must not be null");
        this.operationRepository = Objects.requireNonNull(operationRepository, "operationRepository must not be null");
    }

    @Transactional
    public PreparedXuiClientLifecycleOperation prepareDisable(Long telegramUserId, UUID provisionId) {
        XuiClientProvision provision = verifiedProvision(telegramUserId, provisionId);
        assertNoOperationInProgress(provision);
        if (provision.getStatus() == XuiProvisionStatus.DISABLED) {
            return new PreparedXuiClientLifecycleOperation(provision, false, true);
        }
        if (provision.getStatus() == XuiProvisionStatus.DELETED) {
            throw new XuiClientDisableNotAllowedException("Deleted Xui client cannot be disabled");
        }
        if (provision.getStatus() == XuiProvisionStatus.DISABLING) {
            return new PreparedXuiClientLifecycleOperation(provision, false, false);
        }
        if (provision.getStatus() != XuiProvisionStatus.ACTIVE
                && provision.getStatus() != XuiProvisionStatus.UNKNOWN
                && provision.getStatus() != XuiProvisionStatus.FAILED) {
            throw new XuiClientDisableNotAllowedException("Xui client provision is not disableable");
        }
        boolean claimed = provisionRepository.transitionStatus(provisionId, provision.getStatus(), XuiProvisionStatus.DISABLING);
        return new PreparedXuiClientLifecycleOperation(find(provisionId), claimed, false);
    }

    @Transactional
    public PreparedXuiClientLifecycleOperation prepareDelete(Long telegramUserId, UUID provisionId, boolean force) {
        XuiClientProvision provision = verifiedProvision(telegramUserId, provisionId);
        assertNoOperationInProgress(provision);
        if (provision.getStatus() == XuiProvisionStatus.DELETED) {
            return new PreparedXuiClientLifecycleOperation(provision, false, true);
        }
        if (provision.getStatus() == XuiProvisionStatus.DELETING) {
            return new PreparedXuiClientLifecycleOperation(provision, false, false);
        }
        if (provision.getStatus() == XuiProvisionStatus.ACTIVE && !force) {
            throw new XuiClientDeleteNotAllowedException("Active Xui client requires force delete");
        }
        if (provision.getStatus() != XuiProvisionStatus.ACTIVE
                && provision.getStatus() != XuiProvisionStatus.DISABLED
                && provision.getStatus() != XuiProvisionStatus.UNKNOWN
                && provision.getStatus() != XuiProvisionStatus.FAILED) {
            throw new XuiClientDeleteNotAllowedException("Xui client provision is not deleteable");
        }
        boolean claimed = provisionRepository.transitionStatus(provisionId, provision.getStatus(), XuiProvisionStatus.DELETING);
        return new PreparedXuiClientLifecycleOperation(find(provisionId), claimed, false);
    }

    @Transactional
    public XuiClientProvision markDisabled(UUID provisionId, Instant now) {
        XuiClientProvision provision = find(provisionId);
        provision.markDisabled(now);
        return provisionRepository.save(provision);
    }

    @Transactional
    public XuiClientProvision markDeleted(UUID provisionId, Instant now) {
        XuiClientProvision provision = find(provisionId);
        provision.markDeleted(now);
        return provisionRepository.save(provision);
    }

    @Transactional
    public XuiClientProvision markUnknown(UUID provisionId, String code, String message) {
        XuiClientProvision provision = find(provisionId);
        provision.markOperationUnknown(code, message);
        return provisionRepository.save(provision);
    }

    @Transactional
    public XuiClientProvision markFailed(UUID provisionId, String code, String message) {
        XuiClientProvision provision = find(provisionId);
        provision.markOperationFailed(code, message);
        return provisionRepository.save(provision);
    }

    @Transactional(readOnly = true)
    public XuiClientProvision find(UUID provisionId) {
        return provisionRepository.findById(provisionId)
                .orElseThrow(() -> new XuiClientProvisionNotFoundException(provisionId));
    }

    private XuiClientProvision verifiedProvision(Long telegramUserId, UUID provisionId) {
        User user = userRepository.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new UserNotFoundException(telegramUserId));
        XuiClientProvision provision = provisionRepository.findByIdForUpdate(provisionId)
                .orElseThrow(() -> new XuiClientProvisionNotFoundException(provisionId));
        if (!provision.getUserId().equals(user.getId())) {
            throw new XuiProvisionOwnershipException();
        }
        return provision;
    }

    private void assertNoOperationInProgress(XuiClientProvision provision) {
        if (operationRepository.existsByProvisionIdAndStatus(provision.getId(), XuiClientOperationStatus.IN_PROGRESS)) {
            throw new XuiClientOperationInProgressException();
        }
    }
}
