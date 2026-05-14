package com.kisquant.strategy.application;

import com.kisquant.position.application.PositionResetReason;
import com.kisquant.position.application.StrategyPositionResetter;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.TradeMode;
import com.kisquant.strategy.domain.Strategy;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminStrategyApplicationService {

    private final StrategyRepository strategyRepository;
    private final StrategyPositionResetter positionResetter;
    private final StrategyIdGenerator strategyIdGenerator;

    public AdminStrategyApplicationService(
            StrategyRepository strategyRepository,
            StrategyPositionResetter positionResetter,
            StrategyIdGenerator strategyIdGenerator
    ) {
        this.strategyRepository = strategyRepository;
        this.positionResetter = positionResetter;
        this.strategyIdGenerator = strategyIdGenerator;
    }

    @Transactional(readOnly = true)
    public List<Strategy> findStrategies() {
        return strategyRepository.findAll();
    }

    @Transactional
    public Strategy createStrategy(CreateStrategyCommand command) {
        Strategy strategy = Strategy.create(
                strategyIdGenerator.nextStrategyId(),
                command.name(),
                command.tradeMode(),
                command.initialBudget(),
                command.maxOrderAmount(),
                command.maxDailyOrderAmount());
        return strategyRepository.save(strategy);
    }

    @Transactional
    public Strategy changeEnabled(StrategyId strategyId, boolean enabled) {
        Strategy strategy = findStrategy(strategyId);
        if (enabled) {
            strategy.activate();
        } else {
            strategy.deactivate();
        }
        return strategyRepository.save(strategy);
    }

    @Transactional
    public Strategy changeTradeMode(StrategyId strategyId, TradeMode tradeMode) {
        Strategy strategy = findStrategy(strategyId);
        if (strategy.tradeMode() != tradeMode) {
            positionResetter.resetPositions(strategyId, PositionResetReason.TRADE_MODE_CHANGE);
        }
        strategy.changeTradeMode(tradeMode);
        return strategyRepository.save(strategy);
    }

    @Transactional
    public Strategy changeInitialBudget(StrategyId strategyId, Money initialBudget) {
        Strategy strategy = findStrategy(strategyId);
        strategy.changeInitialBudget(initialBudget);
        return strategyRepository.save(strategy);
    }

    @Transactional
    public void resetPositions(StrategyId strategyId) {
        findStrategy(strategyId);
        positionResetter.resetPositions(strategyId, PositionResetReason.MANUAL);
    }

    private Strategy findStrategy(StrategyId strategyId) {
        return strategyRepository.findById(strategyId)
                .orElseThrow(() -> new IllegalArgumentException("strategy not found"));
    }
}
