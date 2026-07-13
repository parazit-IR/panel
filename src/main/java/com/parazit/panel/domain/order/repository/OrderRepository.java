package com.parazit.panel.domain.order.repository;

import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.OrderType;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends UuidRepository<Order> {

    default Optional<Order> findByIdForUpdate(UUID id) {
        return findById(id);
    }

    List<Order> findAllByUserId(UUID userId);

    Optional<Order> findByPlanSelectionId(UUID planSelectionId);

    Optional<Order> findActiveByTargetSubscriptionIdAndType(UUID targetSubscriptionId, OrderType type);

    boolean existsPaidOrCompletedByPlanSelectionId(UUID planSelectionId);

    boolean existsActiveByTargetSubscriptionIdAndType(UUID targetSubscriptionId, OrderType type);
}
