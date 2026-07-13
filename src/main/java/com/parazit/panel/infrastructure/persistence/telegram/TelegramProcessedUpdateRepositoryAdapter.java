package com.parazit.panel.infrastructure.persistence.telegram;

import com.parazit.panel.domain.telegram.TelegramProcessedUpdate;
import com.parazit.panel.domain.telegram.TelegramUpdateProcessingStatus;
import com.parazit.panel.domain.telegram.repository.TelegramProcessedUpdateRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TelegramProcessedUpdateRepositoryAdapter implements TelegramProcessedUpdateRepository {

    private final SpringDataTelegramProcessedUpdateRepository repository;

    public TelegramProcessedUpdateRepositoryAdapter(SpringDataTelegramProcessedUpdateRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public TelegramProcessedUpdate save(TelegramProcessedUpdate update) {
        return repository.saveAndFlush(Objects.requireNonNull(update, "update must not be null"));
    }

    @Override
    public Optional<TelegramProcessedUpdate> findByUpdateId(long updateId) {
        return repository.findById(updateId);
    }

    @Override
    public Optional<TelegramProcessedUpdate> findByUpdateIdForUpdate(long updateId) {
        return repository.findByUpdateIdForUpdate(updateId);
    }

    @Override
    public boolean existsProcessed(long updateId) {
        return repository.existsByUpdateIdAndStatus(updateId, TelegramUpdateProcessingStatus.PROCESSED);
    }

    @Override
    public List<TelegramProcessedUpdate> findRetryable(Instant now, int limit) {
        Objects.requireNonNull(now, "now must not be null");
        if (limit <= 0) {
            return List.of();
        }
        return repository.findRetryable(List.of(
                TelegramUpdateProcessingStatus.RECEIVED,
                TelegramUpdateProcessingStatus.FAILED
        ), limit);
    }
}
