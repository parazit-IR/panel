package com.parazit.panel.domain.telegram.repository;

import com.parazit.panel.domain.telegram.TelegramSensitiveAction;
import java.util.Optional;
import java.util.UUID;

public interface TelegramSensitiveActionRepository {

    TelegramSensitiveAction save(TelegramSensitiveAction action);

    Optional<TelegramSensitiveAction> findById(UUID id);

    Optional<TelegramSensitiveAction> findByIdForUpdate(UUID id);
}
