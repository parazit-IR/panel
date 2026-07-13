package com.parazit.panel.application.telegram;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.config.properties.TelegramBotProperties;
import com.parazit.panel.domain.telegram.TelegramProcessedUpdate;
import com.parazit.panel.domain.telegram.TelegramUpdateProcessingStatus;
import com.parazit.panel.domain.telegram.repository.TelegramProcessedUpdateRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimTelegramUpdateTransaction {

    private final TelegramProcessedUpdateRepository repository;
    private final SystemClockPort clock;
    private final TelegramBotProperties properties;

    public ClaimTelegramUpdateTransaction(
            TelegramProcessedUpdateRepository repository,
            SystemClockPort clock,
            TelegramBotProperties properties
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Transactional
    public ClaimTelegramUpdateResult claim(long updateId, String handlerKey, Instant receivedAt) {
        TelegramProcessedUpdate update = repository.findByUpdateIdForUpdate(updateId)
                .orElseGet(() -> create(updateId, receivedAt));
        if (update.getStatus() == TelegramUpdateProcessingStatus.PROCESSED) {
            return ClaimTelegramUpdateResult.ALREADY_PROCESSED;
        }
        if (update.getStatus() == TelegramUpdateProcessingStatus.DEAD) {
            return ClaimTelegramUpdateResult.DEAD;
        }
        if (update.getStatus() == TelegramUpdateProcessingStatus.PROCESSING) {
            return ClaimTelegramUpdateResult.BUSY;
        }
        if (!update.canClaim(properties.maxRetryAttempts())) {
            return ClaimTelegramUpdateResult.DEAD;
        }
        update.claim(clock.now(), handlerKey, properties.maxRetryAttempts());
        repository.save(update);
        return ClaimTelegramUpdateResult.CLAIMED;
    }

    private TelegramProcessedUpdate create(long updateId, Instant receivedAt) {
        try {
            return repository.save(TelegramProcessedUpdate.receive(updateId, receivedAt));
        } catch (DataIntegrityViolationException exception) {
            return repository.findByUpdateIdForUpdate(updateId)
                    .orElseThrow(() -> exception);
        }
    }
}
