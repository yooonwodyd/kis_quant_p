package com.kisquant.position.application;

import com.kisquant.position.domain.StrategyPosition;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import java.util.List;
import java.util.Optional;

public interface StrategyPositionRepository {

    StrategyPosition save(StrategyPosition position);

    Optional<StrategyPosition> findByStrategyIdAndSymbol(StrategyId strategyId, Symbol symbol);

    List<StrategyPosition> findByStrategyId(StrategyId strategyId);

    List<StrategyPosition> findAll();

    void deleteByStrategyId(StrategyId strategyId);
}
