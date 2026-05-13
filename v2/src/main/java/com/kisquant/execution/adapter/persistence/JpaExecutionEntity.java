package com.kisquant.execution.adapter.persistence;

import com.kisquant.execution.domain.Execution;
import com.kisquant.execution.domain.ExecutionDedupKey;
import com.kisquant.execution.domain.ExecutionId;
import com.kisquant.execution.domain.ExecutionSource;
import com.kisquant.order.domain.OrderSide;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "executions")
class JpaExecutionEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long strategyId;

    @Column(nullable = false, length = 6)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExecutionSource executionSource;

    @Column(nullable = false)
    private Long executedQuantity;

    @Column(nullable = false)
    private Long executedPrice;

    @Column(nullable = false)
    private Long executedAmount;

    @Column(nullable = false)
    private Long fee = 0L;

    @Column(nullable = false)
    private Long tax = 0L;

    @Column(nullable = false)
    private Instant executedAt;

    @Column(nullable = false, unique = true, length = 200)
    private String dedupKey;

    @Column(nullable = false)
    private Instant createdAt;

    protected JpaExecutionEntity() {
    }

    static JpaExecutionEntity from(Execution execution) {
        JpaExecutionEntity entity = new JpaExecutionEntity();
        entity.id = execution.id().value();
        entity.orderId = execution.orderId().value();
        entity.strategyId = execution.strategyId().value();
        entity.symbol = execution.symbol().value();
        entity.side = execution.side();
        entity.executionSource = execution.source();
        entity.executedQuantity = execution.executedQuantity();
        entity.executedPrice = execution.executedPrice().amount();
        entity.executedAmount = execution.executedAmount().amount();
        entity.executedAt = execution.executedAt();
        entity.dedupKey = execution.dedupKey().value();
        return entity;
    }

    Execution toDomain() {
        return new Execution(
                ExecutionId.of(id),
                OrderId.of(orderId),
                StrategyId.of(strategyId),
                Symbol.of(symbol),
                side,
                executionSource,
                executedQuantity,
                Money.won(executedPrice),
                Money.won(executedAmount),
                executedAt,
                ExecutionDedupKey.of(dedupKey));
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
