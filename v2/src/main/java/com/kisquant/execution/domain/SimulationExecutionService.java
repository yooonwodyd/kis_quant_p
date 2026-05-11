package com.kisquant.execution.domain;

import com.kisquant.order.domain.Order;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.TradeMode;
import java.time.Instant;
import java.util.Objects;

public final class SimulationExecutionService {

    public Execution execute(Order order, ExecutionId executionId, Instant executedAt) {
        return execute(order, executionId, order.orderPrice(), executedAt);
    }

    public Execution execute(Order order, ExecutionId executionId, Money executionPrice, Instant executedAt) {
        Order targetOrder = Objects.requireNonNull(order, "order must not be null");
        Instant executionTime = Objects.requireNonNull(executedAt, "executedAt must not be null");
        if (targetOrder.tradeMode() != TradeMode.SIMULATION) {
            throw new IllegalArgumentException("only simulation orders can be executed");
        }

        Execution execution = Execution.create(
                executionId,
                targetOrder.id(),
                targetOrder.strategyId(),
                targetOrder.symbol(),
                targetOrder.side(),
                ExecutionSource.SIMULATION,
                targetOrder.quantity().value(),
                executionPrice,
                executionTime,
                ExecutionDedupKey.of("SIMULATION-" + targetOrder.id().value()));
        targetOrder.fillBySimulation(executionTime);
        return execution;
    }
}
