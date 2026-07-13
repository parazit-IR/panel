package com.parazit.panel.infrastructure.persistence.xui.provisioning;

import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class XuiClientProvisionRepositoryAdapter
        extends JpaRepositoryAdapter<XuiClientProvision, UUID>
        implements XuiClientProvisionRepository {

    private final SpringDataXuiClientProvisionRepository repository;

    public XuiClientProvisionRepositoryAdapter(SpringDataXuiClientProvisionRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public XuiClientProvision save(XuiClientProvision provision) {
        return repository.saveAndFlush(Objects.requireNonNull(provision, "provision must not be null"));
    }

    @Override
    public Optional<XuiClientProvision> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(Objects.requireNonNull(id, "id must not be null"));
    }

    @Override
    public Optional<XuiClientProvision> findByPlanSelectionId(UUID planSelectionId) {
        return repository.findByPlanSelectionId(Objects.requireNonNull(planSelectionId, "planSelectionId must not be null"));
    }

    @Override
    public Optional<XuiClientProvision> findByRemoteClientId(String remoteClientId) {
        return repository.findByRemoteClientId(Objects.requireNonNull(remoteClientId, "remoteClientId must not be null"));
    }

    @Override
    public Optional<XuiClientProvision> findByRemoteEmail(String remoteEmail) {
        return repository.findByRemoteEmail(Objects.requireNonNull(remoteEmail, "remoteEmail must not be null"));
    }

    @Override
    public List<XuiClientProvision> findAllByUserId(UUID userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(Objects.requireNonNull(userId, "userId must not be null"));
    }

    @Override
    public boolean existsByPlanSelectionId(UUID planSelectionId) {
        return repository.existsByPlanSelectionId(Objects.requireNonNull(planSelectionId, "planSelectionId must not be null"));
    }

    @Override
    @Transactional
    public boolean transitionStatus(
            UUID provisionId,
            XuiProvisionStatus expectedStatus,
            XuiProvisionStatus newStatus
    ) {
        int updated = repository.transitionStatus(
                Objects.requireNonNull(provisionId, "provisionId must not be null"),
                Objects.requireNonNull(expectedStatus, "expectedStatus must not be null"),
                Objects.requireNonNull(newStatus, "newStatus must not be null")
        );
        return updated == 1;
    }
}
