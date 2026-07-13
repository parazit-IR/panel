package com.parazit.panel.domain.telegram.repository;

import com.parazit.panel.domain.telegram.TelegramProcessedUpdate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TelegramProcessedUpdateRepository {

    TelegramProcessedUpdate save(TelegramProcessedUpdate update);

    Optional<TelegramProcessedUpdate> findByUpdateId(long updateId);

    Optional<TelegramProcessedUpdate> findByUpdateIdForUpdate(long updateId);

    boolean existsProcessed(long updateId);

    List<TelegramProcessedUpdate> findRetryable(Instant now, int limit);
}
