package com.kisquant.order.adapter.persistence;

import com.kisquant.order.domain.OrderStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaOrderRepository extends JpaRepository<JpaOrderEntity, Long> {

    boolean existsByStrategyIdAndSymbolAndStatusIn(Long strategyId, String symbol, Collection<OrderStatus> statuses);

    List<JpaOrderEntity> findByStatusInOrderByLastSyncedAtAsc(Collection<OrderStatus> statuses, Pageable pageable);
}
