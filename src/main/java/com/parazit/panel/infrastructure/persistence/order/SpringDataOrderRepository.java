package com.parazit.panel.infrastructure.persistence.order;

import com.parazit.panel.domain.order.Order;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataOrderRepository extends SpringDataUuidRepository<Order> {

    List<Order> findAllByUserId(UUID userId);
}
