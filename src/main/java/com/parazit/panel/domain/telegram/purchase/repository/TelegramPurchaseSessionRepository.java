package com.parazit.panel.domain.telegram.purchase.repository;

import com.parazit.panel.domain.repository.UuidRepository;
import com.parazit.panel.domain.telegram.purchase.PurchaseFlowType;
import com.parazit.panel.domain.telegram.purchase.TelegramPurchaseSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TelegramPurchaseSessionRepository extends UuidRepository<TelegramPurchaseSession> {

    Optional<TelegramPurchaseSession> findByIdForUpdate(UUID id);

    Optional<TelegramPurchaseSession> findActiveByUserId(UUID userId);

    Optional<TelegramPurchaseSession> findActiveByUserIdAndFlowType(UUID userId, PurchaseFlowType flowType);

    List<TelegramPurchaseSession> findAllActiveByUserId(UUID userId);

    List<TelegramPurchaseSession> findAllActiveByUserIdAndFlowType(UUID userId, PurchaseFlowType flowType);

    Optional<TelegramPurchaseSession> findByPlanSelectionId(UUID planSelectionId);

    Optional<TelegramPurchaseSession> findActiveByUserIdAndFlowTypeAndTargetSubscriptionId(
            UUID userId,
            PurchaseFlowType flowType,
            UUID targetSubscriptionId
    );
}
