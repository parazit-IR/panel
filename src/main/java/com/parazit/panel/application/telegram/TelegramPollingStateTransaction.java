package com.parazit.panel.application.telegram;

import com.parazit.panel.config.properties.TelegramBotProperties;
import com.parazit.panel.domain.telegram.TelegramPollingState;
import com.parazit.panel.domain.telegram.repository.TelegramPollingStateRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramPollingStateTransaction {

    private final TelegramPollingStateRepository repository;
    private final TelegramBotProperties properties;

    public TelegramPollingStateTransaction(TelegramPollingStateRepository repository, TelegramBotProperties properties) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Transactional
    public long currentOffset() {
        TelegramPollingState state = repository.findByBotIdentityForUpdate(properties.botIdentity())
                .orElseGet(() -> repository.save(TelegramPollingState.create(properties.botIdentity())));
        return state.getNextOffset();
    }

    @Transactional
    public void advanceAfter(long updateId) {
        TelegramPollingState state = repository.findByBotIdentityForUpdate(properties.botIdentity())
                .orElseGet(() -> repository.save(TelegramPollingState.create(properties.botIdentity())));
        state.advanceAfter(updateId);
        repository.save(state);
    }
}
