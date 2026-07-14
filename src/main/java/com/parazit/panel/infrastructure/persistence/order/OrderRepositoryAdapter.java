package com.parazit.panel.infrastructure.persistence.order;

import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.OrderStatus;
import com.parazit.panel.domain.order.OrderType;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryAdapter extends JpaRepositoryAdapter<Order, UUID> implements OrderRepository {

    private static final List<OrderStatus> ACTIVE_ORDER_STATUSES = List.of(
            OrderStatus.CREATED,
            OrderStatus.PAYMENT_PENDING,
            OrderStatus.PAID,
            OrderStatus.RENEWAL_PENDING,
            OrderStatus.RENEWAL_REVIEW_REQUIRED,
            OrderStatus.PROVISIONING,
            OrderStatus.PROVISIONING_FAILED
    );

    private final SpringDataOrderRepository repository;

    public OrderRepositoryAdapter(SpringDataOrderRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Order save(Order order) {
        return repository.saveAndFlush(Objects.requireNonNull(order, "order must not be null"));
    }

    @Override
    public Optional<Order> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(Objects.requireNonNull(id, "id must not be null"));
    }

    @Override
    public List<Order> findAllByUserId(UUID userId) {
        return repository.findAllByUserId(Objects.requireNonNull(userId, "userId must not be null"));
    }

    @Override
    public Optional<Order> findByPlanSelectionId(UUID planSelectionId) {
        return repository.findByPlanSelectionId(
                Objects.requireNonNull(planSelectionId, "planSelectionId must not be null")
        );
    }

    @Override
    public Optional<Order> findActiveByTargetSubscriptionIdAndType(UUID targetSubscriptionId, OrderType type) {
        return repository.findFirstByTargetSubscriptionIdAndTypeAndStatusInOrderByCreatedAtDesc(
                Objects.requireNonNull(targetSubscriptionId, "targetSubscriptionId must not be null"),
                Objects.requireNonNull(type, "type must not be null"),
                ACTIVE_ORDER_STATUSES
        );
    }

    @Override
    public boolean existsPaidOrCompletedByPlanSelectionId(UUID planSelectionId) {
        return repository.existsByPlanSelectionIdAndStatusIn(
                Objects.requireNonNull(planSelectionId, "planSelectionId must not be null"),
                List.of(OrderStatus.PAID, OrderStatus.PROVISIONING, OrderStatus.COMPLETED)
        );
    }

    @Override
    public boolean existsActiveByTargetSubscriptionIdAndType(UUID targetSubscriptionId, OrderType type) {
        return repository.existsByTargetSubscriptionIdAndTypeAndStatusIn(
                Objects.requireNonNull(targetSubscriptionId, "targetSubscriptionId must not be null"),
                Objects.requireNonNull(type, "type must not be null"),
                ACTIVE_ORDER_STATUSES
        );
    }
}
