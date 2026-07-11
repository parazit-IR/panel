package com.parazit.panel.infrastructure.persistence.xui.operation;

import com.parazit.panel.domain.xui.operation.XuiClientOperation;
import com.parazit.panel.domain.xui.operation.XuiClientOperationStatus;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataXuiClientOperationRepository extends SpringDataUuidRepository<XuiClientOperation> {

    Optional<XuiClientOperation> findByOperationId(UUID operationId);

    List<XuiClientOperation> findAllByProvisionIdOrderByRequestedAtDesc(UUID provisionId);

    boolean existsByOperationId(UUID operationId);

    boolean existsByProvisionIdAndStatus(UUID provisionId, XuiClientOperationStatus status);
}
