package com.kisquant.position.adapter.persistence;

import com.kisquant.position.application.StrategyPositionRepository;
import com.kisquant.position.domain.StrategyPosition;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaStrategyPositionRepositoryAdapter implements StrategyPositionRepository {

    private final JpaStrategyPositionRepository repository;

    public JpaStrategyPositionRepositoryAdapter(JpaStrategyPositionRepository repository) {
        this.repository = repository;
    }

    @Override
    public StrategyPosition save(StrategyPosition position) {
        return repository.saveAndFlush(JpaStrategyPositionEntity.from(position)).toDomain();
    }

    @Override
    public Optional<StrategyPosition> findByStrategyIdAndSymbol(StrategyId strategyId, Symbol symbol) {
        return repository.findById(new JpaStrategyPositionId(strategyId.value(), symbol.value()))
                .map(JpaStrategyPositionEntity::toDomain);
    }

    @Override
    public List<StrategyPosition> findByStrategyId(StrategyId strategyId) {
        return repository.findByIdStrategyId(strategyId.value()).stream()
                .map(JpaStrategyPositionEntity::toDomain)
                .toList();
    }

    @Override
    public List<StrategyPosition> findAll() {
        return repository.findAll().stream().map(JpaStrategyPositionEntity::toDomain).toList();
    }

    @Override
    public void deleteByStrategyId(StrategyId strategyId) {
        repository.deleteByStrategyId(strategyId.value());
        repository.flush();
    }
}
