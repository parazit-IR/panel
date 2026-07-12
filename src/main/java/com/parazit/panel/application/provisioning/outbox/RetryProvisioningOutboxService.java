package com.parazit.panel.application.provisioning.outbox;

import com.parazit.panel.application.port.in.provisioning.outbox.RetryProvisioningOutboxUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.provisioning.outbox.command.RetryProvisioningOutboxCommand;
import com.parazit.panel.application.provisioning.outbox.result.ProvisioningOutboxResult;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutbox;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutboxStatus;
import com.parazit.panel.domain.provisioning.outbox.repository.ProvisioningOutboxRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetryProvisioningOutboxService implements RetryProvisioningOutboxUseCase {

    private final ProvisioningOutboxRepository outboxRepository;
    private final ProvisioningOutboxResultMapper mapper;
    private final SystemClockPort clock;

    public RetryProvisioningOutboxService(
            ProvisioningOutboxRepository outboxRepository,
            ProvisioningOutboxResultMapper mapper,
            SystemClockPort clock
    ) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public ProvisioningOutboxResult retry(RetryProvisioningOutboxCommand command) {
        ProvisioningOutbox outbox = outboxRepository.findByEventId(command.eventId())
                .orElseThrow(() -> new ProvisioningOutboxNotFoundException(command.eventId()));
        if (outbox.getStatus() != ProvisioningOutboxStatus.FAILED
                && outbox.getStatus() != ProvisioningOutboxStatus.UNKNOWN
                && outbox.getStatus() != ProvisioningOutboxStatus.DEAD) {
            throw new ProvisioningOutboxRetryNotAllowedException("Provisioning outbox cannot be retried in its current status");
        }
        outbox.retryNow(clock.now());
        return mapper.toResult(outboxRepository.save(outbox));
    }
}
