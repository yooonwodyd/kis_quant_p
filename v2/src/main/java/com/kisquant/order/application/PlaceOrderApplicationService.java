package com.kisquant.order.application;

import com.kisquant.execution.application.ExecutionRepository;
import com.kisquant.execution.domain.Execution;
import com.kisquant.execution.domain.SimulationExecutionService;
import com.kisquant.marketdata.application.MarketDataGateway;
import com.kisquant.order.domain.AllowedSymbolPolicy;
import com.kisquant.order.domain.Order;
import com.kisquant.order.domain.OrderEligibilityService;
import com.kisquant.order.domain.OrderQuantity;
import com.kisquant.order.domain.OrderSide;
import com.kisquant.position.application.StrategyPositionRepository;
import com.kisquant.position.domain.PositionUpdateService;
import com.kisquant.position.domain.StrategyPosition;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.shared.domain.TradeMode;
import com.kisquant.shared.time.CurrentTimeProvider;
import com.kisquant.strategy.application.StrategyRepository;
import com.kisquant.strategy.domain.Strategy;
import com.kisquant.strategylog.application.StrategyLogRepository;
import com.kisquant.strategylog.domain.LogLevel;
import com.kisquant.strategylog.domain.StrategyLog;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceOrderApplicationService {

    private final StrategyRepository strategyRepository;
    private final OrderRepository orderRepository;
    private final ExecutionRepository executionRepository;
    private final StrategyPositionRepository positionRepository;
    private final StrategyLogRepository strategyLogRepository;
    private final KisOrderGateway kisOrderGateway;
    private final MarketDataGateway marketDataGateway;
    private final TradingIdGenerator idGenerator;
    private final CurrentTimeProvider timeProvider;
    private final AllowedSymbolPolicy allowedSymbolPolicy;
    private final OrderEligibilityService eligibilityService = new OrderEligibilityService();
    private final SimulationExecutionService simulationExecutionService = new SimulationExecutionService();
    private final PositionUpdateService positionUpdateService = new PositionUpdateService();

    public PlaceOrderApplicationService(
            StrategyRepository strategyRepository,
            OrderRepository orderRepository,
            ExecutionRepository executionRepository,
            StrategyPositionRepository positionRepository,
            StrategyLogRepository strategyLogRepository,
            KisOrderGateway kisOrderGateway,
            MarketDataGateway marketDataGateway,
            TradingIdGenerator idGenerator,
            CurrentTimeProvider timeProvider,
            List<Symbol> allowedSymbols
    ) {
        this.strategyRepository = strategyRepository;
        this.orderRepository = orderRepository;
        this.executionRepository = executionRepository;
        this.positionRepository = positionRepository;
        this.strategyLogRepository = strategyLogRepository;
        this.kisOrderGateway = kisOrderGateway;
        this.marketDataGateway = marketDataGateway;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.allowedSymbolPolicy = AllowedSymbolPolicy.of(Set.copyOf(allowedSymbols));
    }

    @Transactional
    public PlaceOrderResult placeOrder(PlaceOrderCommand command) {
        Strategy strategy = strategyRepository.findById(command.strategyId())
                .orElseThrow(() -> new IllegalArgumentException("strategy not found"));
        StrategyPosition currentPosition = positionRepository
                .findByStrategyIdAndSymbol(command.strategyId(), command.symbol())
                .orElseGet(() -> StrategyPosition.empty(command.strategyId(), command.symbol()));
        if (command.side() == OrderSide.SELL
                && command.quantity() > currentPosition.quantity()) {
            throw new IllegalArgumentException("sell quantity exceeds strategy position");
        }
        Money referenceOrderPrice = referenceOrderPrice(command, strategy.tradeMode());
        Money orderAmount = command.side() == OrderSide.SELL
                ? Money.ZERO
                : referenceOrderPrice.multiply(command.quantity());
        eligibilityService.validate(
                strategy,
                command.symbol(),
                command.side(),
                orderAmount,
                currentPosition.acquisitionAmount(),
                orderRepository.existsOpenOrder(command.strategyId(), command.symbol()),
                allowedSymbolPolicy);

        Instant now = timeProvider.now();
        Order order = Order.requested(
                idGenerator.nextOrderId(),
                command.strategyId(),
                command.symbol(),
                command.side(),
                strategy.tradeMode(),
                command.orderType(),
                OrderQuantity.of(command.quantity()),
                command.orderPrice(),
                now);
        orderRepository.save(order);

        if (strategy.tradeMode() == TradeMode.SIMULATION) {
            return placeSimulationOrder(order, currentPosition, referenceOrderPrice, now);
        }
        return placeLiveOrder(order, command);
    }

    private PlaceOrderResult placeSimulationOrder(Order order, StrategyPosition position, Money referenceOrderPrice, Instant now) {
        Money executionPrice = simulationExecutionPrice(order, referenceOrderPrice);
        Execution execution = simulationExecutionService.execute(order, idGenerator.nextExecutionId(), executionPrice, now);
        executionRepository.save(execution);
        positionUpdateService.apply(position, execution);
        positionRepository.save(position);
        strategyLogRepository.save(StrategyLog.create(
                order.strategyId(),
                LogLevel.INFO,
                "SIMULATION 주문 체결: " + order.symbol().value() + " " + order.quantity().value() + "주 " + executionPrice.amount() + "원",
                "{\"tradeMode\":\"SIMULATION\"}",
                now));
        orderRepository.save(order);
        return new PlaceOrderResult(order.id(), order.status(), null, "simulation filled");
    }

    private Money simulationExecutionPrice(Order order, Money referenceOrderPrice) {
        if (order.orderPrice().isPositive()) {
            return order.orderPrice();
        }
        return referenceOrderPrice;
    }

    private PlaceOrderResult placeLiveOrder(Order order, PlaceOrderCommand command) {
        try {
            KisOrderResult result = kisOrderGateway.placeOrder(new KisOrderCommand(
                    command.symbol(),
                    command.side(),
                    command.orderType(),
                    command.quantity(),
                    command.orderPrice()));
            if (result.status() == KisOrderResultStatus.ACCEPTED) {
                order.accept(result.kisOrderNumber(), result.kisOrderOrgNumber(), timeProvider.now());
            } else {
                order.reject(result.rejectCode(), result.rejectMessage());
            }
        } catch (KisTimeoutException exception) {
            order.markUnknown(exception.getMessage());
        }
        saveLiveOrderResultLog(order);
        orderRepository.save(order);
        return new PlaceOrderResult(
                order.id(),
                order.status(),
                order.kisOrderNumber().map(kisOrderNumber -> kisOrderNumber.value()).orElse(null),
                order.rejectMessage().orElse(null));
    }

    private void saveLiveOrderResultLog(Order order) {
        Instant now = timeProvider.now();
        String message = switch (order.status()) {
            case ACCEPTED -> "LIVE 주문 접수: " + order.symbol().value() + " " + order.side().name()
                    + " " + order.quantity().value() + "주 KIS주문번호 "
                    + order.kisOrderNumber().map(kisOrderNumber -> kisOrderNumber.value()).orElse("");
            case REJECTED -> "LIVE 주문 거절: " + order.symbol().value() + " " + order.side().name()
                    + " " + order.quantity().value() + "주 "
                    + order.rejectMessage().orElse("");
            case UNKNOWN -> "LIVE 주문 상태 확인 필요: " + order.symbol().value() + " " + order.side().name()
                    + " " + order.quantity().value() + "주 "
                    + order.rejectMessage().orElse("");
            default -> "LIVE 주문 결과: " + order.symbol().value() + " " + order.side().name()
                    + " " + order.quantity().value() + "주 " + order.status().name();
        };
        strategyLogRepository.save(StrategyLog.create(
                order.strategyId(),
                LogLevel.INFO,
                message,
                "{\"tradeMode\":\"LIVE\",\"status\":\"" + order.status().name() + "\"}",
                now));
    }

    private Money referenceOrderPrice(PlaceOrderCommand command, TradeMode tradeMode) {
        if (command.orderPrice().isPositive()) {
            return command.orderPrice();
        }
        if (command.side() == OrderSide.SELL && tradeMode == TradeMode.LIVE) {
            return Money.ZERO;
        }
        return marketDataGateway.quote(command.symbol()).currentPrice();
    }
}
