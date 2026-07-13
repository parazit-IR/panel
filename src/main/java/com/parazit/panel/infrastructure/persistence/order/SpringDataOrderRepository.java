package com.parazit.panel.infrastructure.persistence.order;

import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.OrderStatus;
import com.parazit.panel.domain.order.OrderType;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataOrderRepository extends SpringDataUuidRepository<Order> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    List<Order> findAllByUserId(UUID userId);

    Optional<Order> findByPlanSelectionId(UUID planSelectionId);

    Optional<Order> findFirstByTargetSubscriptionIdAndTypeAndStatusInOrderByCreatedAtDesc(
            UUID targetSubscriptionId,
            OrderType type,
            List<OrderStatus> statuses
    );

    boolean existsByPlanSelectionIdAndStatusIn(UUID planSelectionId, List<OrderStatus> statuses);

    boolean existsByTargetSubscriptionIdAndTypeAndStatusIn(UUID targetSubscriptionId, OrderType type, List<OrderStatus> statuses);
}
