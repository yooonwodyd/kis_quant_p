package com.kisquant.position.application;

import com.kisquant.position.domain.StrategyPosition;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StrategyPositionApplicationService {

    private final StrategyPositionRepository repository;

    public StrategyPositionApplicationService(StrategyPositionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<StrategyPosition> findPosition(StrategyId strategyId, Symbol symbol) {
        return repository.findByStrategyIdAndSymbol(strategyId, symbol);
    }

    @Transactional(readOnly = true)
    public List<StrategyPosition> findPositions(StrategyId strategyId) {
        return repository.findByStrategyId(strategyId);
    }

    @Transactional(readOnly = true)
    public List<StrategyPosition> findPositions() {
        return repository.findAll();
    }
}
