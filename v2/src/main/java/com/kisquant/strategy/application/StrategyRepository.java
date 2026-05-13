package com.kisquant.strategy.application;

import com.kisquant.shared.domain.StrategyId;
import com.kisquant.strategy.domain.Strategy;
import java.util.List;
import java.util.Optional;

public interface StrategyRepository {

    Strategy save(Strategy strategy);

    Optional<Strategy> findById(StrategyId strategyId);

    List<Strategy> findAll();
}
