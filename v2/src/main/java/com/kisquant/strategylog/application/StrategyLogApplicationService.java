package com.kisquant.strategylog.application;

import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.time.CurrentTimeProvider;
import com.kisquant.strategylog.domain.StrategyLog;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StrategyLogApplicationService {

    private final StrategyLogRepository repository;
    private final CurrentTimeProvider timeProvider;

    public StrategyLogApplicationService(StrategyLogRepository repository, CurrentTimeProvider timeProvider) {
        this.repository = repository;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public StrategyLog createLog(CreateStrategyLogCommand command) {
        return repository.save(StrategyLog.create(
                command.strategyId(),
                command.level(),
                command.message(),
                command.payloadJson(),
                timeProvider.now()));
    }

    @Transactional(readOnly = true)
    public List<StrategyLog> recentLogs(StrategyId strategyId, int limit) {
        return repository.findRecentByStrategyId(strategyId, limit);
    }

    @Transactional(readOnly = true)
    public List<StrategyLog> recentLogs(int limit) {
        return repository.findRecent(limit);
    }
}
