package com.parazit.panel.infrastructure.persistence.telegram;

import com.parazit.panel.domain.telegram.TelegramProcessedUpdate;
import com.parazit.panel.domain.telegram.TelegramUpdateProcessingStatus;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataRepository;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataTelegramProcessedUpdateRepository
        extends SpringDataRepository<TelegramProcessedUpdate, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select update from TelegramProcessedUpdate update where update.updateId = :updateId")
    Optional<TelegramProcessedUpdate> findByUpdateIdForUpdate(@Param("updateId") Long updateId);

    boolean existsByUpdateIdAndStatus(Long updateId, TelegramUpdateProcessingStatus status);

    @Query("""
            select update from TelegramProcessedUpdate update
            where update.status in :statuses
            order by update.receivedAt asc
            limit :limit
            """)
    List<TelegramProcessedUpdate> findRetryable(
            @Param("statuses") Collection<TelegramUpdateProcessingStatus> statuses,
            @Param("limit") int limit
    );
}
