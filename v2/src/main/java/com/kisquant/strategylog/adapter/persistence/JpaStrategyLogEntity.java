package com.kisquant.strategylog.adapter.persistence;

import com.kisquant.shared.domain.StrategyId;
import com.kisquant.strategylog.domain.LogLevel;
import com.kisquant.strategylog.domain.StrategyLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "strategy_logs")
class JpaStrategyLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long strategyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LogLevel level;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(columnDefinition = "json")
    private String payloadJson;

    @Column(nullable = false)
    private Instant createdAt;

    protected JpaStrategyLogEntity() {
    }

    static JpaStrategyLogEntity from(StrategyLog log) {
        JpaStrategyLogEntity entity = new JpaStrategyLogEntity();
        entity.strategyId = log.strategyId().value();
        entity.level = log.level();
        entity.message = log.message();
        entity.payloadJson = log.payloadJson();
        entity.createdAt = log.createdAt();
        return entity;
    }

    StrategyLog toDomain() {
        return StrategyLog.create(StrategyId.of(strategyId), level, message, payloadJson, createdAt);
    }
}
