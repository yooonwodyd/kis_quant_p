package com.kisquant.position.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.kisquant.account.application.AccountBalance;
import com.kisquant.account.application.AccountGateway;
import com.kisquant.account.application.BuyableOrderAmount;
import com.kisquant.account.application.Holding;
import com.kisquant.execution.application.ExecutionRepository;
import com.kisquant.execution.domain.Execution;
import com.kisquant.execution.domain.ExecutionDedupKey;
import com.kisquant.marketdata.application.DailyCandle;
import com.kisquant.marketdata.application.MarketDataGateway;
import com.kisquant.marketdata.application.Quote;
import com.kisquant.order.application.CancelOrderCommand;
import com.kisquant.order.application.CancelOrderResult;
import com.kisquant.order.application.KisOrderCommand;
import com.kisquant.order.application.KisOrderGateway;
import com.kisquant.order.application.KisOrderResult;
import com.kisquant.order.application.OrderRepository;
import com.kisquant.order.application.PlaceOrderApplicationService;
import com.kisquant.order.application.SequentialTradingIdGenerator;
import com.kisquant.order.domain.KisOrderNumber;
import com.kisquant.order.domain.KisOrderOrgNumber;
import com.kisquant.order.domain.Order;
import com.kisquant.order.domain.OrderSide;
import com.kisquant.order.domain.OrderStatus;
import com.kisquant.order.domain.OrderType;
import com.kisquant.position.domain.StrategyPosition;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.shared.domain.TradeMode;
import com.kisquant.strategy.application.AdminStrategyLogService;
import com.kisquant.strategy.application.StrategyRepository;
import com.kisquant.strategy.domain.Strategy;
import com.kisquant.strategylog.application.StrategyLogRepository;
import com.kisquant.strategylog.domain.StrategyLog;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StrategyPositionResetApplicationServiceTest {

    @Test
    void liveResetPlacesMarketSellForKisOrderableQuantityAndKeepsLocalPosition() {
        InMemoryPositionRepository positions = new InMemoryPositionRepository(StrategyPosition.restore(
                StrategyId.of(1L), Symbol.of("001510"), 3L, Money.won(5_570L), Money.ZERO));
        RecordingAccountGateway account = new RecordingAccountGateway(List.of(holding(3L, 2L)));
        RecordingKisOrderGateway kis = RecordingKisOrderGateway.accepted();
        InMemoryStrategyLogRepository logs = new InMemoryStrategyLogRepository();
        StrategyPositionResetApplicationService service = resetService(liveStrategy(), positions, account, kis, logs);

        service.resetPositions(StrategyId.of(1L), PositionResetReason.MANUAL);

        assertThat(kis.commands).singleElement().satisfies(command -> {
            assertThat(command.symbol()).isEqualTo(Symbol.of("001510"));
            assertThat(command.side()).isEqualTo(OrderSide.SELL);
            assertThat(command.orderType()).isEqualTo(OrderType.MARKET);
            assertThat(command.quantity()).isEqualTo(2L);
            assertThat(command.orderPrice()).isEqualTo(Money.ZERO);
        });
        assertThat(positions.deletedStrategyIds).isEmpty();
        assertThat(positions.position).isNotNull();
        assertThat(logs.saved).anySatisfy(log -> assertThat(log.message()).contains("LIVE", "포지션 정리", "매도"));
    }

    @Test
    void liveResetStoresRejectedSellOrderAndKeepsLocalPosition() {
        InMemoryPositionRepository positions = new InMemoryPositionRepository(StrategyPosition.restore(
                StrategyId.of(1L), Symbol.of("001510"), 1L, Money.won(5_570L), Money.ZERO));
        InMemoryOrderRepository orders = new InMemoryOrderRepository();
        RecordingKisOrderGateway kis = RecordingKisOrderGateway.rejected("APBK0912", "장운영 시간이 아닙니다");
        InMemoryStrategyLogRepository logs = new InMemoryStrategyLogRepository();
        StrategyPositionResetApplicationService service = resetService(
                liveStrategy(),
                orders,
                positions,
                new RecordingAccountGateway(List.of(holding(1L, 1L))),
                kis,
                logs);

        service.resetPositions(StrategyId.of(1L), PositionResetReason.MANUAL);

        assertThat(orders.findAll()).singleElement()
                .satisfies(order -> assertThat(order.status()).isEqualTo(OrderStatus.REJECTED));
        assertThat(positions.deletedStrategyIds).isEmpty();
        assertThat(positions.position).isNotNull();
        assertThat(logs.saved).anySatisfy(log -> assertThat(log.message()).contains("LIVE 주문 거절", "장운영 시간이 아닙니다"));
        assertThat(logs.saved).anySatisfy(log -> assertThat(log.message()).contains("LIVE", "포지션 정리", "REJECTED"));
    }

    @Test
    void simulationResetDeletesLocalPositionsWithoutCallingKis() {
        InMemoryPositionRepository positions = new InMemoryPositionRepository(StrategyPosition.restore(
                StrategyId.of(1L), Symbol.of("001510"), 1L, Money.won(5_570L), Money.ZERO));
        RecordingAccountGateway account = new RecordingAccountGateway(List.of(holding(1L, 1L)));
        RecordingKisOrderGateway kis = RecordingKisOrderGateway.accepted();
        StrategyPositionResetApplicationService service = resetService(
                simulationStrategy(),
                positions,
                account,
                kis,
                new InMemoryStrategyLogRepository());

        service.resetPositions(StrategyId.of(1L), PositionResetReason.MANUAL);

        assertThat(positions.deletedStrategyIds).containsExactly(StrategyId.of(1L));
        assertThat(account.holdingsCallCount).isZero();
        assertThat(kis.commands).isEmpty();
    }

    private StrategyPositionResetApplicationService resetService(
            Strategy strategy,
            InMemoryPositionRepository positions,
            RecordingAccountGateway account,
            RecordingKisOrderGateway kis,
            InMemoryStrategyLogRepository logs
    ) {
        return resetService(strategy, new InMemoryOrderRepository(), positions, account, kis, logs);
    }

    private StrategyPositionResetApplicationService resetService(
            Strategy strategy,
            InMemoryOrderRepository orders,
            InMemoryPositionRepository positions,
            RecordingAccountGateway account,
            RecordingKisOrderGateway kis,
            InMemoryStrategyLogRepository logs
    ) {
        PlaceOrderApplicationService placeOrderService = new PlaceOrderApplicationService(
                new InMemoryStrategyRepository(strategy),
                orders,
                new InMemoryExecutionRepository(),
                positions,
                logs,
                kis,
                new FixedMarketDataGateway(),
                new SequentialTradingIdGenerator(),
                () -> Instant.parse("2026-05-06T01:00:00Z"),
                List.of(Symbol.of("001510")));
        return new StrategyPositionResetApplicationService(
                new InMemoryStrategyRepository(strategy),
                positions,
                account,
                placeOrderService,
                new AdminStrategyLogService(logs, () -> Instant.parse("2026-05-06T01:00:00Z")));
    }

    private Holding holding(long holdingQuantity, long orderableQuantity) {
        return new Holding(
                Symbol.of("001510"),
                "SK증권",
                holdingQuantity,
                orderableQuantity,
                Money.won(5_570L),
                Money.won(5_570L),
                Money.won(Math.multiplyExact(holdingQuantity, 5_570L)));
    }

    private Strategy liveStrategy() {
        return Strategy.create(StrategyId.of(1L), "수동 실전 투자 전략", TradeMode.LIVE,
                Money.won(30_000L), Money.won(30_000L), Money.won(30_000L));
    }

    private Strategy simulationStrategy() {
        return Strategy.create(StrategyId.of(1L), "수동 모의 투자 전략", TradeMode.SIMULATION,
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

    private static final class InMemoryPositionRepository implements StrategyPositionRepository {
        private StrategyPosition position;
        private final List<StrategyId> deletedStrategyIds = new ArrayList<>();

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
            return Optional.ofNullable(position)
                    .filter(target -> target.strategyId().equals(strategyId) && target.symbol().equals(symbol));
        }

        @Override
        public List<StrategyPosition> findByStrategyId(StrategyId strategyId) {
            return Optional.ofNullable(position)
                    .filter(target -> target.strategyId().equals(strategyId))
                    .map(List::of)
                    .orElseGet(List::of);
        }

        @Override
        public List<StrategyPosition> findAll() {
            return position == null ? List.of() : List.of(position);
        }

        @Override
        public void deleteByStrategyId(StrategyId strategyId) {
            deletedStrategyIds.add(strategyId);
            if (position != null && position.strategyId().equals(strategyId)) {
                position = null;
            }
        }
    }

    private static final class RecordingAccountGateway implements AccountGateway {
        private final List<Holding> holdings;
        private int holdingsCallCount;

        private RecordingAccountGateway(List<Holding> holdings) {
            this.holdings = holdings;
        }

        @Override
        public AccountBalance balance() {
            return new AccountBalance(Money.ZERO);
        }

        @Override
        public List<Holding> holdings() {
            holdingsCallCount++;
            return holdings;
        }

        @Override
        public BuyableOrderAmount buyable(Symbol symbol, Money price) {
            return new BuyableOrderAmount(symbol, Money.ZERO, 0L);
        }
    }

    private static final class InMemoryOrderRepository implements OrderRepository {
        private Order order;

        @Override
        public Order save(Order order) {
            this.order = order;
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
        @Override
        public Execution save(Execution execution) {
            return execution;
        }

        @Override
        public boolean existsByDedupKey(ExecutionDedupKey dedupKey) {
            return false;
        }

        @Override
        public List<Execution> findByOrderId(OrderId orderId) {
            return List.of();
        }

        @Override
        public List<Execution> findAll() {
            return List.of();
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
        private final KisOrderResult result;
        private final List<KisOrderCommand> commands = new ArrayList<>();

        private RecordingKisOrderGateway(KisOrderResult result) {
            this.result = result;
        }

        static RecordingKisOrderGateway accepted() {
            return new RecordingKisOrderGateway(
                    KisOrderResult.accepted(KisOrderNumber.of("0007103300"), KisOrderOrgNumber.of("06010")));
        }

        static RecordingKisOrderGateway rejected(String code, String message) {
            return new RecordingKisOrderGateway(KisOrderResult.rejected(code, message));
        }

        @Override
        public KisOrderResult placeOrder(KisOrderCommand command) {
            commands.add(command);
            return result;
        }

        @Override
        public CancelOrderResult cancel(CancelOrderCommand command) {
            return CancelOrderResult.succeeded();
        }
    }

    private static final class FixedMarketDataGateway implements MarketDataGateway {
        @Override
        public Quote quote(Symbol symbol) {
            return new Quote(symbol, Money.won(5_570L));
        }

        @Override
        public List<DailyCandle> dailyCandles(Symbol symbol) {
            return List.of();
        }
    }
}
