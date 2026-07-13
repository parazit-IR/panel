package com.parazit.panel.application.telegram;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.domain.telegram.TelegramProcessedUpdate;
import com.parazit.panel.domain.telegram.repository.TelegramProcessedUpdateRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompleteTelegramUpdateTransaction {

    private final TelegramProcessedUpdateRepository repository;
    private final SystemClockPort clock;

    public CompleteTelegramUpdateTransaction(TelegramProcessedUpdateRepository repository, SystemClockPort clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public void complete(long updateId, String responseFingerprint) {
        TelegramProcessedUpdate update = repository.findByUpdateIdForUpdate(updateId)
                .orElseThrow(() -> new IllegalStateException("Telegram update claim is missing"));
        update.markProcessed(clock.now(), responseFingerprint);
        repository.save(update);
    }
}
