package com.parazit.panel.domain.order.repository;

import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends UuidRepository<Order> {

    List<Order> findAllByUserId(UUID userId);
}
