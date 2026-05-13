package com.kisquant.order.application;

import com.kisquant.order.domain.Order;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId orderId);

    boolean existsOpenOrder(StrategyId strategyId, Symbol symbol);

    List<Order> findPollingTargets(int batchSize);

    List<Order> findAll();
}
