package com.parazit.panel.infrastructure.persistence.telegram;

import com.parazit.panel.domain.telegram.purchase.TelegramPurchaseSession;
import com.parazit.panel.domain.telegram.purchase.TelegramPurchaseSessionStatus;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataTelegramPurchaseSessionRepository
        extends SpringDataUuidRepository<TelegramPurchaseSession> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from TelegramPurchaseSession session where session.id = :id")
    Optional<TelegramPurchaseSession> findByIdForUpdate(@Param("id") UUID id);

    Optional<TelegramPurchaseSession> findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
            UUID userId,
            Collection<TelegramPurchaseSessionStatus> statuses
    );

    List<TelegramPurchaseSession> findAllByUserIdAndStatusInOrderByCreatedAtDesc(
            UUID userId,
            Collection<TelegramPurchaseSessionStatus> statuses
    );

    Optional<TelegramPurchaseSession> findFirstByPlanSelectionIdOrderByCreatedAtDesc(UUID planSelectionId);
}
