package com.kisquant.position.adapter.persistence;

import com.kisquant.position.domain.StrategyPosition;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "strategy_positions")
class JpaStrategyPositionEntity {

    @EmbeddedId
    private JpaStrategyPositionId id;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false)
    private Long avgPrice;

    @Column(nullable = false)
    private Long realizedPnl;

    @Column(nullable = false)
    private Instant updatedAt;

    protected JpaStrategyPositionEntity() {
    }

    static JpaStrategyPositionEntity from(StrategyPosition position) {
        JpaStrategyPositionEntity entity = new JpaStrategyPositionEntity();
        entity.id = new JpaStrategyPositionId(position.strategyId().value(), position.symbol().value());
        entity.quantity = position.quantity();
        entity.avgPrice = position.avgPrice().amount();
        entity.realizedPnl = position.realizedPnl().amount();
        return entity;
    }

    StrategyPosition toDomain() {
        return StrategyPosition.restore(
                StrategyId.of(id.strategyId()),
                Symbol.of(id.symbol()),
                quantity,
                Money.won(avgPrice),
                Money.won(realizedPnl));
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
