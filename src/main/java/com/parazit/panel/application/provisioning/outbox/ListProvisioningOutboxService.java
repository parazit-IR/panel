package com.parazit.panel.application.provisioning.outbox;

import com.parazit.panel.application.port.in.provisioning.outbox.ListProvisioningOutboxUseCase;
import com.parazit.panel.application.provisioning.outbox.query.ListProvisioningOutboxQuery;
import com.parazit.panel.application.provisioning.outbox.result.ProvisioningOutboxResult;
import com.parazit.panel.domain.provisioning.outbox.repository.ProvisioningOutboxRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListProvisioningOutboxService implements ListProvisioningOutboxUseCase {

    private final ProvisioningOutboxRepository outboxRepository;
    private final ProvisioningOutboxResultMapper mapper;

    public ListProvisioningOutboxService(
            ProvisioningOutboxRepository outboxRepository,
            ProvisioningOutboxResultMapper mapper
    ) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProvisioningOutboxResult> list(ListProvisioningOutboxQuery query) {
        int limit = Math.max(1, Math.min(query.limit() <= 0 ? 50 : query.limit(), 200));
        return outboxRepository.findAllOrderByCreatedAtDesc(limit)
                .stream()
                .map(mapper::toResult)
                .toList();
    }
}
