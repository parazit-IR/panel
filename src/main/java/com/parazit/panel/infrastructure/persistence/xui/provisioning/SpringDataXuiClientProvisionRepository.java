package com.parazit.panel.infrastructure.persistence.xui.provisioning;

import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataXuiClientProvisionRepository extends SpringDataUuidRepository<XuiClientProvision> {

    Optional<XuiClientProvision> findByPlanSelectionId(UUID planSelectionId);

    Optional<XuiClientProvision> findByRemoteClientId(String remoteClientId);

    Optional<XuiClientProvision> findByRemoteEmail(String remoteEmail);

    List<XuiClientProvision> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByPlanSelectionId(UUID planSelectionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update XuiClientProvision provision
            set provision.status = :newStatus
            where provision.id = :provisionId
              and provision.status = :expectedStatus
            """)
    int transitionStatus(
            @Param("provisionId") UUID provisionId,
            @Param("expectedStatus") XuiProvisionStatus expectedStatus,
            @Param("newStatus") XuiProvisionStatus newStatus
    );
}
