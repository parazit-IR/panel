package com.parazit.panel.domain.xui.provisioning.repository;

import com.parazit.panel.domain.repository.UuidRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface XuiClientProvisionRepository extends UuidRepository<XuiClientProvision> {

    Optional<XuiClientProvision> findByPlanSelectionId(UUID planSelectionId);

    Optional<XuiClientProvision> findByRemoteClientId(String remoteClientId);

    Optional<XuiClientProvision> findByRemoteEmail(String remoteEmail);

    List<XuiClientProvision> findAllByUserId(UUID userId);

    boolean existsByPlanSelectionId(UUID planSelectionId);

    boolean transitionStatus(UUID provisionId, XuiProvisionStatus expectedStatus, XuiProvisionStatus newStatus);
}
