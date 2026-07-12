package com.parazit.panel.application.provisioning.outbox;

import com.parazit.panel.application.port.in.provisioning.outbox.GetProvisioningOutboxUseCase;
import com.parazit.panel.application.provisioning.outbox.query.GetProvisioningOutboxQuery;
import com.parazit.panel.application.provisioning.outbox.result.ProvisioningOutboxResult;
import com.parazit.panel.domain.provisioning.outbox.repository.ProvisioningOutboxRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetProvisioningOutboxService implements GetProvisioningOutboxUseCase {

    private final ProvisioningOutboxRepository outboxRepository;
    private final ProvisioningOutboxResultMapper mapper;

    public GetProvisioningOutboxService(
            ProvisioningOutboxRepository outboxRepository,
            ProvisioningOutboxResultMapper mapper
    ) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public ProvisioningOutboxResult get(GetProvisioningOutboxQuery query) {
        return mapper.toResult(outboxRepository.findByEventId(query.eventId())
                .orElseThrow(() -> new ProvisioningOutboxNotFoundException(query.eventId())));
    }
}
