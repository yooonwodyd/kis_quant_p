package com.kisquant.position.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
record JpaStrategyPositionId(
        @Column(name = "strategy_id", nullable = false) Long strategyId,
        @Column(name = "symbol", nullable = false, length = 6) String symbol
) implements Serializable {
}
