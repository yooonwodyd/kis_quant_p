package com.kisquant.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kisquant.marketdata.application.DailyCandle;
import com.kisquant.marketdata.application.MarketDataGateway;
import com.kisquant.marketdata.application.Quote;
import com.kisquant.execution.application.ExecutionRepository;
import com.kisquant.execution.domain.Execution;
import com.kisquant.execution.domain.ExecutionDedupKey;
import com.kisquant.order.domain.KisOrderNumber;
import com.kisquant.order.domain.KisOrderOrgNumber;
import com.kisquant.order.domain.Order;
import com.kisquant.order.domain.OrderSide;
import com.kisquant.order.domain.OrderStatus;
import com.kisquant.order.domain.OrderType;
import com.kisquant.position.application.StrategyPositionRepository;
import com.kisquant.position.domain.StrategyPosition;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.shared.domain.TradeMode;
import com.kisquant.strategy.application.StrategyRepository;
import com.kisquant.strategy.domain.Strategy;
import com.kisquant.strategylog.application.StrategyLogRepository;
import com.kisquant.strategylog.domain.StrategyLog;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlaceOrderApplicationServiceTest {

    @Test
    void liveOrderSavesRequestedOrderBeforeCallingKisAndThenAcceptsResult() {
        InMemoryStrategyRepository strategies = new InMemoryStrategyRepository(liveStrategy());
        InMemoryOrderRepository orders = new InMemoryOrderRepository();
        InMemoryStrategyLogRepository logs = new InMemoryStrategyLogRepository();
        RecordingKisOrderGateway kis = RecordingKisOrderGateway.accepted();
        PlaceOrderApplicationService service = service(strategies, orders, new InMemoryExecutionRepository(),
                new InMemoryPositionRepository(), logs, kis);

        PlaceOrderResult result = service.placeOrder(limitBuyCommand());

        assertThat(kis.called).isTrue();
        assertThat(orders.savedStatuses).containsExactly(OrderStatus.REQUESTED, OrderStatus.ACCEPTED);
        assertThat(result.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(result.kisOrderNumber()).contains("0007103300");
        assertThat(logs.saved).singleElement().satisfies(log -> {
            assertThat(log.message()).contains("LIVE", "주문 접수", "001510");
            assertThat(log.payloadJson()).contains("\"status\":\"ACCEPTED\"");
        });
    }

    @Test
    void kisTimeoutMarksOrderUnknownWithoutRetrying() {
        InMemoryOrderRepository orders = new InMemoryOrderRepository();
        RecordingKisOrderGateway kis = RecordingKisOrderGateway.timeout();
        PlaceOrderApplicationService service = service(new InMemoryStrategyRepository(liveStrategy()), orders,
                new InMemoryExecutionRepository(), new InMemoryPositionRepository(), new InMemoryStrategyLogRepository(), kis);

        PlaceOrderResult result = service.placeOrder(limitBuyCommand());

        assertThat(kis.callCount).isEqualTo(1);
        assertThat(result.status()).isEqualTo(OrderStatus.UNKNOWN);
        assertThat(orders.savedStatuses).containsExactly(OrderStatus.REQUESTED, OrderStatus.UNKNOWN);
    }

    @Test
    void simulationOrderDoesNotCallKisAndCreatesExecutionPositionAndLog() {
        InMemoryExecutionRepository executions = new InMemoryExecutionRepository();
        InMemoryPositionRepository positions = new InMemoryPositionRepository();
        InMemoryStrategyLogRepository logs = new InMemoryStrategyLogRepository();
        RecordingKisOrderGateway kis = RecordingKisOrderGateway.accepted();
        PlaceOrderApplicationService service = service(new InMemoryStrategyRepository(simulationStrategy()),
                new InMemoryOrderRepository(), executions, positions, logs, kis);

        PlaceOrderResult result = service.placeOrder(limitBuyCommand());

        assertThat(kis.called).isFalse();
        assertThat(result.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(executions.saved).hasSize(1);
        assertThat(executions.saved.getFirst().source().name()).isEqualTo("SIMULATION");
        assertThat(positions.position.quantity()).isEqualTo(1L);
        assertThat(logs.saved).singleElement().satisfies(log -> assertThat(log.message()).contains("SIMULATION"));
    }

    @Test
    void simulationMarketOrderUsesCurrentQuoteAsExecutionPrice() {
        InMemoryExecutionRepository executions = new InMemoryExecutionRepository();
        InMemoryPositionRepository positions = new InMemoryPositionRepository();
        FixedMarketDataGateway marketData = new FixedMarketDataGateway(Money.won(5_410L));
        PlaceOrderApplicationService service = service(new InMemoryStrategyRepository(simulationStrategy()),
                new InMemoryOrderRepository(), executions, positions, new InMemoryStrategyLogRepository(),
                RecordingKisOrderGateway.accepted(), marketData);

        PlaceOrderResult result = service.placeOrder(marketBuyCommand());

        assertThat(result.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(executions.saved).singleElement()
                .satisfies(execution -> assertThat(execution.executedPrice()).isEqualTo(Money.won(5_410L)));
        assertThat(positions.position.acquisitionAmount()).isEqualTo(Money.won(5_410L));
        assertThat(marketData.quoteCallCount).isEqualTo(1);
    }

    @Test
    void marketOrderBudgetValidationUsesCurrentQuoteInsteadOfMaxOrderAmount() {
        InMemoryExecutionRepository executions = new InMemoryExecutionRepository();
        InMemoryPositionRepository positions = new InMemoryPositionRepository(
                StrategyPosition.restore(StrategyId.of(1L), Symbol.of("001510"), 1L, Money.won(5_410L), Money.ZERO));
        PlaceOrderApplicationService service = service(new InMemoryStrategyRepository(simulationStrategy()),
                new InMemoryOrderRepository(), executions, positions, new InMemoryStrategyLogRepository(),
                RecordingKisOrderGateway.accepted(), new FixedMarketDataGateway(Money.won(5_410L)));

        PlaceOrderResult result = service.placeOrder(marketBuyCommand());

        assertThat(result.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(positions.position.quantity()).isEqualTo(2L);
        assertThat(positions.position.acquisitionAmount()).isEqualTo(Money.won(10_820L));
    }

    @Test
    void liveMarketSellDoesNotFetchQuoteBeforeSendingOrderToKis() {
        InMemoryPositionRepository positions = new InMemoryPositionRepository(
                StrategyPosition.restore(StrategyId.of(1L), Symbol.of("001510"), 1L, Money.won(5_410L), Money.ZERO));
        FixedMarketDataGateway marketData = new FixedMarketDataGateway(Money.won(5_410L));
        RecordingKisOrderGateway kis = RecordingKisOrderGateway.accepted();
        PlaceOrderApplicationService service = service(new InMemoryStrategyRepository(liveStrategy()),
                new InMemoryOrderRepository(), new InMemoryExecutionRepository(), positions,
                new InMemoryStrategyLogRepository(), kis, marketData);

        PlaceOrderResult result = service.placeOrder(marketSellCommand());

        assertThat(result.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(marketData.quoteCallCount).isZero();
        assertThat(kis.lastCommand.orderPrice()).isEqualTo(Money.ZERO);
    }

    @Test
    void inactiveStrategyCannotPlaceOrder() {
        Strategy strategy = simulationStrategy();
        strategy.deactivate();
        PlaceOrderApplicationService service = service(new InMemoryStrategyRepository(strategy), new InMemoryOrderRepository(),
                new InMemoryExecutionRepository(), new InMemoryPositionRepository(), new InMemoryStrategyLogRepository(),
                RecordingKisOrderGateway.accepted());

        assertThatThrownBy(() -> service.placeOrder(limitBuyCommand()))
                .isInstanceOf(IllegalStateException.class);
    }

    private PlaceOrderApplicationService service(
            StrategyRepository strategies,
            InMemoryOrderRepository orders,
            ExecutionRepository executions,
            StrategyPositionRepository positions,
            StrategyLogRepository logs,
            KisOrderGateway kis
    ) {
        return service(strategies, orders, executions, positions, logs, kis, new FixedMarketDataGateway(Money.won(1_000L)));
    }

    private PlaceOrderApplicationService service(
            StrategyRepository strategies,
            InMemoryOrderRepository orders,
            ExecutionRepository executions,
            StrategyPositionRepository positions,
            StrategyLogRepository logs,
            KisOrderGateway kis,
            MarketDataGateway marketData
    ) {
        return new PlaceOrderApplicationService(
                strategies,
                orders,
                executions,
                positions,
                logs,
                kis,
                marketData,
                new SequentialTradingIdGenerator(),
                () -> Instant.parse("2026-05-06T01:00:00Z"),
                List.of(Symbol.of("001510")));
    }

    private PlaceOrderCommand limitBuyCommand() {
        return new PlaceOrderCommand(StrategyId.of(1L), Symbol.of("001510"), OrderSide.BUY, OrderType.LIMIT, 1L, Money.won(1_000L));
    }

    private PlaceOrderCommand marketBuyCommand() {
        return new PlaceOrderCommand(StrategyId.of(1L), Symbol.of("001510"), OrderSide.BUY, OrderType.MARKET, 1L, Money.ZERO);
    }

    private PlaceOrderCommand marketSellCommand() {
        return new PlaceOrderCommand(StrategyId.of(1L), Symbol.of("001510"), OrderSide.SELL, OrderType.MARKET, 1L, Money.ZERO);
    }

    private Strategy liveStrategy() {
        return Strategy.create(StrategyId.of(1L), "수동 주문 테스트 전략", TradeMode.LIVE,
                Money.won(30_000L), Money.won(30_000L), Money.won(30_000L));
    }

    private Strategy simulationStrategy() {
        return Strategy.create(StrategyId.of(1L), "수동 주문 테스트 전략", TradeMode.SIMULATION,
                Money.won(30_000L), Money.won(30_000L), Money.won(30_000L));
    }

    private static final class InMemoryStrategyRepository implements StrategyRepository {
        private final Strategy strategy;

        private InMemoryStrategyRepository(Strategy strategy) {
            this.strategy = strategy;
        }

        @Override
        public Strategy save(Strategy strategy) {
            return strategy;
        }

        @Override
        public Optional<Strategy> findById(StrategyId strategyId) {
            return Optional.of(strategy);
        }

        @Override
        public List<Strategy> findAll() {
            return List.of(strategy);
        }
    }

    private static final class InMemoryOrderRepository implements OrderRepository {
        private Order order;
        private final List<OrderStatus> savedStatuses = new ArrayList<>();

        @Override
        public Order save(Order order) {
            this.order = order;
            this.savedStatuses.add(order.status());
            return order;
        }

        @Override
        public Optional<Order> findById(OrderId orderId) {
            return Optional.ofNullable(order);
        }

        @Override
        public boolean existsOpenOrder(StrategyId strategyId, Symbol symbol) {
            return false;
        }

        @Override
        public List<Order> findPollingTargets(int batchSize) {
            return List.of();
        }

        @Override
        public List<Order> findAll() {
            return order == null ? List.of() : List.of(order);
        }
    }

    private static final class InMemoryExecutionRepository implements ExecutionRepository {
        private final List<Execution> saved = new ArrayList<>();

        @Override
        public Execution save(Execution execution) {
            saved.add(execution);
            return execution;
        }

        @Override
        public boolean existsByDedupKey(ExecutionDedupKey dedupKey) {
            return false;
        }

        @Override
        public List<Execution> findByOrderId(OrderId orderId) {
            return saved.stream()
                    .filter(execution -> execution.orderId().equals(orderId))
                    .toList();
        }

        @Override
        public List<Execution> findAll() {
            return saved;
        }
    }

    private static final class InMemoryPositionRepository implements StrategyPositionRepository {
        private StrategyPosition position;

        private InMemoryPositionRepository() {
        }

        private InMemoryPositionRepository(StrategyPosition position) {
            this.position = position;
        }

        @Override
        public StrategyPosition save(StrategyPosition position) {
            this.position = position;
            return position;
        }

        @Override
        public Optional<StrategyPosition> findByStrategyIdAndSymbol(StrategyId strategyId, Symbol symbol) {
            return Optional.ofNullable(position);
        }

        @Override
        public List<StrategyPosition> findByStrategyId(StrategyId strategyId) {
            return position == null ? List.of() : List.of(position);
        }

        @Override
        public List<StrategyPosition> findAll() {
            return position == null ? List.of() : List.of(position);
        }

        @Override
        public void deleteByStrategyId(StrategyId strategyId) {
            if (position != null && position.strategyId().equals(strategyId)) {
                position = null;
            }
        }
    }

    private static final class InMemoryStrategyLogRepository implements StrategyLogRepository {
        private final List<StrategyLog> saved = new ArrayList<>();

        @Override
        public StrategyLog save(StrategyLog log) {
            saved.add(log);
            return log;
        }

        @Override
        public List<StrategyLog> findRecentByStrategyId(StrategyId strategyId, int limit) {
            return saved;
        }

        @Override
        public List<StrategyLog> findRecent(int limit) {
            return saved;
        }
    }

    private static final class RecordingKisOrderGateway implements KisOrderGateway {
        private final boolean timeout;
        private boolean called;
        private int callCount;
        private KisOrderCommand lastCommand;

        private RecordingKisOrderGateway(boolean timeout) {
            this.timeout = timeout;
        }

        static RecordingKisOrderGateway accepted() {
            return new RecordingKisOrderGateway(false);
        }

        static RecordingKisOrderGateway timeout() {
            return new RecordingKisOrderGateway(true);
        }

        @Override
        public KisOrderResult placeOrder(KisOrderCommand command) {
            called = true;
            callCount++;
            lastCommand = command;
            if (timeout) {
                throw new KisTimeoutException("timeout");
            }
            return KisOrderResult.accepted(KisOrderNumber.of("0007103300"), KisOrderOrgNumber.of("06010"));
        }

        @Override
        public CancelOrderResult cancel(CancelOrderCommand command) {
            return CancelOrderResult.succeeded();
        }
    }

    private static final class FixedMarketDataGateway implements MarketDataGateway {
        private final Money currentPrice;
        private int quoteCallCount;

        private FixedMarketDataGateway(Money currentPrice) {
            this.currentPrice = currentPrice;
        }

        @Override
        public Quote quote(Symbol symbol) {
            quoteCallCount++;
            return new Quote(symbol, currentPrice);
        }

        @Override
        public List<DailyCandle> dailyCandles(Symbol symbol) {
            return List.of();
        }
    }
}
