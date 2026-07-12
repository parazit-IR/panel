package com.parazit.panel.infrastructure.persistence.order;

import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryAdapter extends JpaRepositoryAdapter<Order, UUID> implements OrderRepository {

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
    public List<Order> findAllByUserId(UUID userId) {
        return repository.findAllByUserId(Objects.requireNonNull(userId, "userId must not be null"));
    }
}
