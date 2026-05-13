package com.kisquant.strategylog.application;

import com.kisquant.shared.domain.StrategyId;
import com.kisquant.strategylog.domain.StrategyLog;
import java.util.List;

public interface StrategyLogRepository {

    StrategyLog save(StrategyLog log);

    List<StrategyLog> findRecentByStrategyId(StrategyId strategyId, int limit);

    List<StrategyLog> findRecent(int limit);
}
