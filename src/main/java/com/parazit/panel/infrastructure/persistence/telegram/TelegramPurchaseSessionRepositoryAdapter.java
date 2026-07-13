package com.parazit.panel.infrastructure.persistence.telegram;

import com.parazit.panel.domain.telegram.purchase.TelegramPurchaseSession;
import com.parazit.panel.domain.telegram.purchase.PurchaseFlowType;
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
        return findActiveByUserIdAndFlowType(userId, PurchaseFlowType.NEW_SUBSCRIPTION);
    }

    @Override
    public Optional<TelegramPurchaseSession> findActiveByUserIdAndFlowType(UUID userId, PurchaseFlowType flowType) {
        return repository.findFirstByUserIdAndFlowTypeAndStatusInOrderByCreatedAtDesc(
                Objects.requireNonNull(userId, "userId must not be null"),
                Objects.requireNonNull(flowType, "flowType must not be null"),
                ACTIVE_STATUSES
        );
    }

    @Override
    public List<TelegramPurchaseSession> findAllActiveByUserId(UUID userId) {
        return findAllActiveByUserIdAndFlowType(userId, PurchaseFlowType.NEW_SUBSCRIPTION);
    }

    @Override
    public List<TelegramPurchaseSession> findAllActiveByUserIdAndFlowType(UUID userId, PurchaseFlowType flowType) {
        return repository.findAllByUserIdAndFlowTypeAndStatusInOrderByCreatedAtDesc(
                Objects.requireNonNull(userId, "userId must not be null"),
                Objects.requireNonNull(flowType, "flowType must not be null"),
                ACTIVE_STATUSES
        );
    }

    @Override
    public Optional<TelegramPurchaseSession> findByPlanSelectionId(UUID planSelectionId) {
        return repository.findFirstByPlanSelectionIdOrderByCreatedAtDesc(
                Objects.requireNonNull(planSelectionId, "planSelectionId must not be null")
        );
    }

    @Override
    public Optional<TelegramPurchaseSession> findActiveByUserIdAndFlowTypeAndTargetSubscriptionId(
            UUID userId,
            PurchaseFlowType flowType,
            UUID targetSubscriptionId
    ) {
        return repository.findFirstByUserIdAndFlowTypeAndTargetSubscriptionIdAndStatusInOrderByCreatedAtDesc(
                Objects.requireNonNull(userId, "userId must not be null"),
                Objects.requireNonNull(flowType, "flowType must not be null"),
                Objects.requireNonNull(targetSubscriptionId, "targetSubscriptionId must not be null"),
                ACTIVE_STATUSES
        );
    }
}
