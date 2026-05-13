package com.kisquant.order.adapter.persistence;

import com.kisquant.order.application.OrderRepository;
import com.kisquant.order.domain.Order;
import com.kisquant.order.domain.OrderStatus;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class JpaOrderRepositoryAdapter implements OrderRepository {

    private static final Set<OrderStatus> OPEN_STATUSES = EnumSet.of(
            OrderStatus.REQUESTED,
            OrderStatus.ACCEPTED,
            OrderStatus.PARTIALLY_FILLED,
            OrderStatus.UNKNOWN);

    private final JpaOrderRepository repository;

    public JpaOrderRepositoryAdapter(JpaOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Order save(Order order) {
        JpaOrderEntity entity = repository.findById(order.id().value())
                .orElseGet(() -> JpaOrderEntity.from(order));
        entity.updateFrom(order);
        return repository.saveAndFlush(entity).toDomain();
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return repository.findById(orderId.value()).map(JpaOrderEntity::toDomain);
    }

    @Override
    public boolean existsOpenOrder(StrategyId strategyId, Symbol symbol) {
        return repository.existsByStrategyIdAndSymbolAndStatusIn(strategyId.value(), symbol.value(), OPEN_STATUSES);
    }

    @Override
    public List<Order> findPollingTargets(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batch size must be positive");
        }
        Set<OrderStatus> pollingStatuses = EnumSet.of(
                OrderStatus.ACCEPTED,
                OrderStatus.PARTIALLY_FILLED,
                OrderStatus.UNKNOWN);
        return repository.findByStatusInOrderByLastSyncedAtAsc(pollingStatuses, PageRequest.of(0, batchSize))
                .stream()
                .map(JpaOrderEntity::toDomain)
                .filter(Order::isPollingTarget)
                .toList();
    }

    @Override
    public List<Order> findAll() {
        return repository.findAll().stream().map(JpaOrderEntity::toDomain).toList();
    }
}
