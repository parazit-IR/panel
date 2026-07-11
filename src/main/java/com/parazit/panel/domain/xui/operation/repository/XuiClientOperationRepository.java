package com.parazit.panel.domain.xui.operation.repository;

import com.parazit.panel.domain.repository.UuidRepository;
import com.parazit.panel.domain.xui.operation.XuiClientOperation;
import com.parazit.panel.domain.xui.operation.XuiClientOperationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface XuiClientOperationRepository extends UuidRepository<XuiClientOperation> {

    Optional<XuiClientOperation> findByOperationId(UUID operationId);

    List<XuiClientOperation> findAllByProvisionIdOrderByRequestedAtDesc(UUID provisionId);

    boolean existsByOperationId(UUID operationId);

    boolean existsByProvisionIdAndStatus(UUID provisionId, XuiClientOperationStatus status);
}
