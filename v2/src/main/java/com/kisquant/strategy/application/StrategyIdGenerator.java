package com.kisquant.strategy.application;

import com.kisquant.shared.domain.StrategyId;

@FunctionalInterface
public interface StrategyIdGenerator {

    StrategyId nextStrategyId();
}
