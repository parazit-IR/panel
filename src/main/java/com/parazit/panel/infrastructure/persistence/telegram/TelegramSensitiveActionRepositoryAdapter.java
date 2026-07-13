package com.parazit.panel.infrastructure.persistence.telegram;

import com.parazit.panel.domain.telegram.TelegramSensitiveAction;
import com.parazit.panel.domain.telegram.repository.TelegramSensitiveActionRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class TelegramSensitiveActionRepositoryAdapter
        extends JpaRepositoryAdapter<TelegramSensitiveAction, UUID>
        implements TelegramSensitiveActionRepository {

    private final SpringDataTelegramSensitiveActionRepository repository;

    public TelegramSensitiveActionRepositoryAdapter(SpringDataTelegramSensitiveActionRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public TelegramSensitiveAction save(TelegramSensitiveAction action) {
        return repository.saveAndFlush(Objects.requireNonNull(action, "action must not be null"));
    }

    @Override
    public Optional<TelegramSensitiveAction> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(Objects.requireNonNull(id, "id must not be null"));
    }
}
