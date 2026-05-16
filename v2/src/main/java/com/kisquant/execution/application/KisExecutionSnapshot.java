package com.kisquant.execution.application;

import com.kisquant.order.domain.KisOrderNumber;
import com.kisquant.shared.domain.Money;
import java.time.Instant;
import java.util.Objects;

public record KisExecutionSnapshot(
        KisOrderNumber kisOrderNumber,
        long cumulativeExecutedQuantity,
        Money cumulativeExecutedAmount,
        Money averagePrice,
        Instant executedAt
) {

    public KisExecutionSnapshot {
        Objects.requireNonNull(kisOrderNumber, "kisOrderNumber must not be null");
        if (cumulativeExecutedQuantity <= 0L) {
            throw new IllegalArgumentException("cumulative executed quantity must be positive");
        }
        Objects.requireNonNull(cumulativeExecutedAmount, "cumulativeExecutedAmount must not be null");
        if (!cumulativeExecutedAmount.isPositive()) {
            throw new IllegalArgumentException("cumulative executed amount must be positive");
        }
        Objects.requireNonNull(averagePrice, "averagePrice must not be null");
        if (!averagePrice.isPositive()) {
            throw new IllegalArgumentException("average price must be positive");
        }
        Objects.requireNonNull(executedAt, "executedAt must not be null");
    }
}
