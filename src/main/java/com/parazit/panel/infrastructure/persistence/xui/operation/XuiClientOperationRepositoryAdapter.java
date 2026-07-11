package com.parazit.panel.infrastructure.persistence.xui.operation;

import com.parazit.panel.domain.xui.operation.XuiClientOperation;
import com.parazit.panel.domain.xui.operation.XuiClientOperationStatus;
import com.parazit.panel.domain.xui.operation.repository.XuiClientOperationRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class XuiClientOperationRepositoryAdapter
        extends JpaRepositoryAdapter<XuiClientOperation, UUID>
        implements XuiClientOperationRepository {

    private final SpringDataXuiClientOperationRepository repository;

    public XuiClientOperationRepositoryAdapter(SpringDataXuiClientOperationRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public XuiClientOperation save(XuiClientOperation operation) {
        return repository.saveAndFlush(Objects.requireNonNull(operation, "operation must not be null"));
    }

    @Override
    public Optional<XuiClientOperation> findByOperationId(UUID operationId) {
        return repository.findByOperationId(Objects.requireNonNull(operationId, "operationId must not be null"));
    }

    @Override
    public List<XuiClientOperation> findAllByProvisionIdOrderByRequestedAtDesc(UUID provisionId) {
        return repository.findAllByProvisionIdOrderByRequestedAtDesc(
                Objects.requireNonNull(provisionId, "provisionId must not be null")
        );
    }

    @Override
    public boolean existsByOperationId(UUID operationId) {
        return repository.existsByOperationId(Objects.requireNonNull(operationId, "operationId must not be null"));
    }

    @Override
    public boolean existsByProvisionIdAndStatus(UUID provisionId, XuiClientOperationStatus status) {
        return repository.existsByProvisionIdAndStatus(
                Objects.requireNonNull(provisionId, "provisionId must not be null"),
                Objects.requireNonNull(status, "status must not be null")
        );
    }
}
