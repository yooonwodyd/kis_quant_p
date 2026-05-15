package com.kisquant.position.application;

import com.kisquant.account.application.AccountGateway;
import com.kisquant.account.application.Holding;
import com.kisquant.order.application.PlaceOrderApplicationService;
import com.kisquant.order.application.PlaceOrderCommand;
import com.kisquant.order.application.PlaceOrderResult;
import com.kisquant.order.domain.OrderSide;
import com.kisquant.order.domain.OrderType;
import com.kisquant.position.domain.StrategyPosition;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.shared.domain.TradeMode;
import com.kisquant.strategy.application.AdminStrategyLogService;
import com.kisquant.strategy.application.StrategyRepository;
import com.kisquant.strategy.domain.Strategy;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StrategyPositionResetApplicationService implements StrategyPositionResetter {

    private final StrategyRepository strategyRepository;
    private final StrategyPositionRepository positionRepository;
    private final AccountGateway accountGateway;
    private final PlaceOrderApplicationService placeOrderService;
    private final AdminStrategyLogService logService;

    public StrategyPositionResetApplicationService(
            StrategyRepository strategyRepository,
            StrategyPositionRepository positionRepository,
            AccountGateway accountGateway,
            PlaceOrderApplicationService placeOrderService,
            AdminStrategyLogService logService
    ) {
        this.strategyRepository = strategyRepository;
        this.positionRepository = positionRepository;
        this.accountGateway = accountGateway;
        this.placeOrderService = placeOrderService;
        this.logService = logService;
    }

    @Override
    @Transactional
    public void resetPositions(StrategyId strategyId, PositionResetReason reason) {
        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(() -> new IllegalArgumentException("strategy not found"));
        if (strategy.tradeMode() == TradeMode.SIMULATION) {
            resetSimulationPositions(strategyId, reason);
            return;
        }
        liquidateLivePositions(strategyId, reason);
    }

    private void resetSimulationPositions(StrategyId strategyId, PositionResetReason reason) {
        try {
            positionRepository.deleteByStrategyId(strategyId);
            logService.recordPositionResetSuccess(strategyId, reason);
        } catch (RuntimeException exception) {
            logService.recordPositionResetFailure(strategyId, reason.name(), exception);
            throw exception;
        }
    }

    private void liquidateLivePositions(StrategyId strategyId, PositionResetReason reason) {
        Map<Symbol, StrategyPosition> positions = positionRepository.findByStrategyId(strategyId).stream()
                .filter(position -> position.quantity() > 0L)
                .collect(Collectors.toMap(StrategyPosition::symbol, Function.identity()));
        if (positions.isEmpty()) {
            logService.recordLivePositionResetSkipped(strategyId, reason, "정리할 로컬 포지션이 없습니다");
            return;
        }

        Map<Symbol, Holding> holdings = accountGateway.holdings().stream()
                .collect(Collectors.toMap(Holding::symbol, Function.identity(), (left, right) -> left));
        for (StrategyPosition position : positions.values()) {
            Holding holding = holdings.get(position.symbol());
            long sellQuantity = sellQuantity(position, holding);
            if (sellQuantity <= 0L) {
                logService.recordLivePositionResetSkipped(
                        strategyId,
                        reason,
                        position.symbol().value() + " 주문가능 수량이 없습니다");
                continue;
            }
            placeMarketSell(strategyId, reason, position.symbol(), sellQuantity);
        }
    }

    private long sellQuantity(StrategyPosition position, Holding holding) {
        if (holding == null) {
            return 0L;
        }
        return Math.min(position.quantity(), holding.orderableQuantity());
    }

    private void placeMarketSell(StrategyId strategyId, PositionResetReason reason, Symbol symbol, long quantity) {
        try {
            PlaceOrderResult result = placeOrderService.placeOrder(new PlaceOrderCommand(
                    strategyId,
                    symbol,
                    OrderSide.SELL,
                    OrderType.MARKET,
                    quantity,
                    Money.ZERO));
            logService.recordLivePositionResetOrderResult(strategyId, reason, symbol, quantity, result.status(), result.message());
        } catch (RuntimeException exception) {
            logService.recordPositionResetFailure(strategyId, reason.name(), exception);
            throw exception;
        }
    }
}
