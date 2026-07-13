package com.parazit.panel.application.telegram;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.config.properties.TelegramBotProperties;
import com.parazit.panel.domain.telegram.TelegramProcessedUpdate;
import com.parazit.panel.domain.telegram.repository.TelegramProcessedUpdateRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FailTelegramUpdateTransaction {

    private final TelegramProcessedUpdateRepository repository;
    private final SystemClockPort clock;
    private final TelegramBotProperties properties;

    public FailTelegramUpdateTransaction(
            TelegramProcessedUpdateRepository repository,
            SystemClockPort clock,
            TelegramBotProperties properties
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Transactional
    public void fail(long updateId, String failureCode, String failureMessage) {
        TelegramProcessedUpdate update = repository.findByUpdateIdForUpdate(updateId)
                .orElseThrow(() -> new IllegalStateException("Telegram update claim is missing"));
        update.markFailed(clock.now(), failureCode, failureMessage, properties.maxRetryAttempts());
        repository.save(update);
    }
}
