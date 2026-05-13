package com.kisquant.strategylog.adapter.persistence;

import com.kisquant.shared.domain.StrategyId;
import com.kisquant.strategylog.application.StrategyLogRepository;
import com.kisquant.strategylog.domain.StrategyLog;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class JpaStrategyLogRepositoryAdapter implements StrategyLogRepository {

    private final JpaStrategyLogRepository repository;

    public JpaStrategyLogRepositoryAdapter(JpaStrategyLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public StrategyLog save(StrategyLog log) {
        return repository.saveAndFlush(JpaStrategyLogEntity.from(log)).toDomain();
    }

    @Override
    public List<StrategyLog> findRecentByStrategyId(StrategyId strategyId, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return repository.findByStrategyIdOrderByCreatedAtDesc(strategyId.value(), PageRequest.of(0, limit))
                .stream()
                .map(JpaStrategyLogEntity::toDomain)
                .toList();
    }

    @Override
    public List<StrategyLog> findRecent(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(JpaStrategyLogEntity::toDomain)
                .toList();
    }
}
