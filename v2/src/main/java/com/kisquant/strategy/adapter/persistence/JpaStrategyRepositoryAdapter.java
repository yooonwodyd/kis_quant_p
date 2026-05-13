package com.kisquant.strategy.adapter.persistence;

import com.kisquant.shared.domain.StrategyId;
import com.kisquant.strategy.application.StrategyRepository;
import com.kisquant.strategy.domain.Strategy;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaStrategyRepositoryAdapter implements StrategyRepository {

    private final JpaStrategyRepository repository;

    public JpaStrategyRepositoryAdapter(JpaStrategyRepository repository) {
        this.repository = repository;
    }

    @Override
    public Strategy save(Strategy strategy) {
        JpaStrategyEntity entity = repository.findById(strategy.id().value())
                .orElseGet(() -> JpaStrategyEntity.from(strategy));
        entity.updateFrom(strategy);
        return repository.saveAndFlush(entity).toDomain();
    }

    @Override
    public Optional<Strategy> findById(StrategyId strategyId) {
        return repository.findById(strategyId.value()).map(JpaStrategyEntity::toDomain);
    }

    @Override
    public List<Strategy> findAll() {
        return repository.findAll().stream().map(JpaStrategyEntity::toDomain).toList();
    }
}
