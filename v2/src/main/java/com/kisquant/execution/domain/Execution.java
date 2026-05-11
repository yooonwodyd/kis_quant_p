package com.kisquant.execution.domain;

import com.kisquant.order.domain.OrderSide;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import java.time.Instant;
import java.util.Objects;

/*
체결 도메인
ExecutionSource source를 비롯해서, 모의투자에 대한 도메인 분리가 필요한지 고민 필요.
체결의 경우 생성자 단계에서 검증.
 */
public record Execution(
        ExecutionId id,
        OrderId orderId,
        StrategyId strategyId,
        Symbol symbol,
        OrderSide side,
        ExecutionSource source,
        long executedQuantity,
        Money executedPrice,
        Money executedAmount,
        Instant executedAt, // 타임 스템프 변경.
        ExecutionDedupKey dedupKey
) {

    public Execution {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(side, "side must not be null");
        Objects.requireNonNull(source, "source must not be null");
        if (executedQuantity <= 0L) {
            throw new IllegalArgumentException("executed quantity must be positive");
        }
        Objects.requireNonNull(executedPrice, "executedPrice must not be null");
        if (!executedPrice.isPositive()) {
            throw new IllegalArgumentException("executed price must be positive");
        }
        Objects.requireNonNull(executedAmount, "executedAmount must not be null");
        Objects.requireNonNull(executedAt, "executedAt must not be null");
        Objects.requireNonNull(dedupKey, "dedupKey must not be null");
    }

    public static Execution create(
            ExecutionId id,
            OrderId orderId,
            StrategyId strategyId,
            Symbol symbol,
            OrderSide side,
            ExecutionSource source,
            long executedQuantity,
            Money executedPrice,
            Instant executedAt,
            ExecutionDedupKey dedupKey
    ) {
        return new Execution(
                id,
                orderId,
                strategyId,
                symbol,
                side,
                source,
                executedQuantity,
                executedPrice,
                executedPrice.multiply(executedQuantity),
                executedAt,
                dedupKey);
    }

    public static Execution createWithAmount(
            ExecutionId id,
            OrderId orderId,
            StrategyId strategyId,
            Symbol symbol,
            OrderSide side,
            ExecutionSource source,
            long executedQuantity,
            Money executedPrice,
            Money executedAmount,
            Instant executedAt,
            ExecutionDedupKey dedupKey
    ) {
        return new Execution(
                id,
                orderId,
                strategyId,
                symbol,
                side,
                source,
                executedQuantity,
                executedPrice,
                executedAmount,
                executedAt,
                dedupKey);
    }
}
