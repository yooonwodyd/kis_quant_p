package com.kisquant.order.application;

import com.kisquant.order.domain.Order;
import com.kisquant.shared.domain.OrderId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderQueryService {

    private final OrderRepository repository;

    public OrderQueryService(OrderRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Order> findOrders() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Order> findOrder(OrderId orderId) {
        return repository.findById(orderId);
    }
}
