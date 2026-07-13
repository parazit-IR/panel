package com.parazit.panel.infrastructure.persistence.telegram;

import com.parazit.panel.domain.telegram.TelegramSensitiveAction;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataTelegramSensitiveActionRepository
        extends SpringDataUuidRepository<TelegramSensitiveAction> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select action from TelegramSensitiveAction action where action.id = :id")
    Optional<TelegramSensitiveAction> findByIdForUpdate(@Param("id") UUID id);
}
