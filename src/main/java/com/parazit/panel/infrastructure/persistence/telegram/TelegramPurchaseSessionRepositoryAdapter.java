package com.parazit.panel.infrastructure.persistence.telegram;

import com.parazit.panel.domain.telegram.purchase.TelegramPurchaseSession;
import com.parazit.panel.domain.telegram.purchase.TelegramPurchaseSessionStatus;
import com.parazit.panel.domain.telegram.purchase.repository.TelegramPurchaseSessionRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class TelegramPurchaseSessionRepositoryAdapter
        extends JpaRepositoryAdapter<TelegramPurchaseSession, UUID>
        implements TelegramPurchaseSessionRepository {

    private static final List<TelegramPurchaseSessionStatus> ACTIVE_STATUSES = List.of(
            TelegramPurchaseSessionStatus.PLAN_SELECTED,
            TelegramPurchaseSessionStatus.PRE_INVOICE_SHOWN,
            TelegramPurchaseSessionStatus.ORDER_CREATED,
            TelegramPurchaseSessionStatus.PAYMENT_METHODS_SHOWN,
            TelegramPurchaseSessionStatus.PAYMENT_CREATED
    );

    private final SpringDataTelegramPurchaseSessionRepository repository;

    public TelegramPurchaseSessionRepositoryAdapter(SpringDataTelegramPurchaseSessionRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public TelegramPurchaseSession save(TelegramPurchaseSession session) {
        return repository.saveAndFlush(Objects.requireNonNull(session, "session must not be null"));
    }

    @Override
    public Optional<TelegramPurchaseSession> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(Objects.requireNonNull(id, "id must not be null"));
    }

    @Override
    public Optional<TelegramPurchaseSession> findActiveByUserId(UUID userId) {
        return repository.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
                Objects.requireNonNull(userId, "userId must not be null"),
                ACTIVE_STATUSES
        );
    }

    @Override
    public List<TelegramPurchaseSession> findAllActiveByUserId(UUID userId) {
        return repository.findAllByUserIdAndStatusInOrderByCreatedAtDesc(
                Objects.requireNonNull(userId, "userId must not be null"),
                ACTIVE_STATUSES
        );
    }

    @Override
    public Optional<TelegramPurchaseSession> findByPlanSelectionId(UUID planSelectionId) {
        return repository.findFirstByPlanSelectionIdOrderByCreatedAtDesc(
                Objects.requireNonNull(planSelectionId, "planSelectionId must not be null")
        );
    }
}
