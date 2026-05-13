package com.kisquant.strategy.adapter.persistence;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.TradeMode;
import com.kisquant.strategy.domain.Strategy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "strategies")
class JpaStrategyEntity {

    @Id
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TradeMode tradeMode;

    @Column(nullable = false)
    private Long initialBudgetAmount;

    @Column(nullable = false)
    private Long maxOrderAmount;

    @Column(nullable = false)
    private Long maxDailyOrderAmount;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected JpaStrategyEntity() {
    }

    static JpaStrategyEntity from(Strategy strategy) {
        JpaStrategyEntity entity = new JpaStrategyEntity();
        entity.updateFrom(strategy);
        return entity;
    }

    void updateFrom(Strategy strategy) {
        id = strategy.id().value();
        name = strategy.name();
        status = strategy.enabled() ? "ACTIVE" : "INACTIVE";
        enabled = strategy.enabled();
        tradeMode = strategy.tradeMode();
        initialBudgetAmount = strategy.initialBudget().amount();
        maxOrderAmount = strategy.maxOrderAmount().amount();
        maxDailyOrderAmount = strategy.maxDailyOrderAmount().amount();
    }

    Strategy toDomain() {
        return Strategy.restore(
                StrategyId.of(id),
                name,
                tradeMode,
                Money.won(initialBudgetAmount),
                Money.won(maxOrderAmount),
                Money.won(maxDailyOrderAmount),
                enabled);
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
