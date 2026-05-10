package com.kisquant.execution.domain;

import java.time.Instant;
import java.util.Objects;

import com.kisquant.order.domain.Order;
import com.kisquant.shared.domain.ExecutionId;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.TradeMode;

/**
 * 모의 주문을 내부 체결로 바꾼다.
 */
public final class SimulationExecutionService {

	public Execution execute(Order order, ExecutionId executionId, Instant executedAt) {
		Order targetOrder = Objects.requireNonNull(order, "order must not be null");
		return execute(targetOrder, executionId, targetOrder.orderPrice(), executedAt);
	}

	public Execution execute(Order order, ExecutionId executionId, Money executionPrice, Instant executedAt) {
		Order targetOrder = Objects.requireNonNull(order, "order must not be null");
		if (targetOrder.tradeMode() != TradeMode.SIMULATION) {
			throw new IllegalArgumentException("only simulation order can be executed here");
		}
		Instant executionTime = Objects.requireNonNull(executedAt, "executedAt must not be null");
		Execution execution = Execution.create(
				Objects.requireNonNull(executionId, "executionId must not be null"),
				targetOrder.id(),
				targetOrder.strategyId(),
				targetOrder.symbol(),
				targetOrder.side(),
				ExecutionSource.SIMULATION,
				targetOrder.quantity().value(),
				Objects.requireNonNull(executionPrice, "executionPrice must not be null"),
				executionTime,
				ExecutionDedupKey.of("SIMULATION-" + targetOrder.id().value())
		);
		targetOrder.fillBySimulation(executionTime);
		return execution;
	}
}
