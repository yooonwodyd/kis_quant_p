package com.kisquant.position.application;

import com.kisquant.shared.domain.StrategyId;

public interface StrategyPositionResetter {

    void resetPositions(StrategyId strategyId, PositionResetReason reason);
}
