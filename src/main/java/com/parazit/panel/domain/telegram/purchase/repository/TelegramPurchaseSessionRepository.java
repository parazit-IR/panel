package com.parazit.panel.domain.telegram.purchase.repository;

import com.parazit.panel.domain.repository.UuidRepository;
import com.parazit.panel.domain.telegram.purchase.TelegramPurchaseSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TelegramPurchaseSessionRepository extends UuidRepository<TelegramPurchaseSession> {

    Optional<TelegramPurchaseSession> findByIdForUpdate(UUID id);

    Optional<TelegramPurchaseSession> findActiveByUserId(UUID userId);

    List<TelegramPurchaseSession> findAllActiveByUserId(UUID userId);

    Optional<TelegramPurchaseSession> findByPlanSelectionId(UUID planSelectionId);
}
