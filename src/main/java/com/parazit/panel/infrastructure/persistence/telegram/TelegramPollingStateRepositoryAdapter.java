package com.parazit.panel.infrastructure.persistence.telegram;

import com.parazit.panel.domain.telegram.TelegramPollingState;
import com.parazit.panel.domain.telegram.repository.TelegramPollingStateRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TelegramPollingStateRepositoryAdapter implements TelegramPollingStateRepository {

    private final SpringDataTelegramPollingStateRepository repository;

    public TelegramPollingStateRepositoryAdapter(SpringDataTelegramPollingStateRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public TelegramPollingState save(TelegramPollingState state) {
        return repository.saveAndFlush(Objects.requireNonNull(state, "state must not be null"));
    }

    @Override
    public Optional<TelegramPollingState> findByBotIdentity(String botIdentity) {
        return repository.findById(requireIdentity(botIdentity));
    }

    @Override
    public Optional<TelegramPollingState> findByBotIdentityForUpdate(String botIdentity) {
        return repository.findByBotIdentityForUpdate(requireIdentity(botIdentity));
    }

    private static String requireIdentity(String botIdentity) {
        Objects.requireNonNull(botIdentity, "botIdentity must not be null");
        return botIdentity;
    }
}
