package com.kisquant.execution.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.kisquant.execution.domain.Execution;
import com.kisquant.execution.domain.ExecutionDedupKey;
import com.kisquant.execution.domain.ExecutionId;
import com.kisquant.execution.domain.ExecutionSource;
import com.kisquant.operationlog.application.OperationLogBuffer;
import com.kisquant.operationlog.domain.OperationLogCategory;
import com.kisquant.operationlog.domain.OperationLogLevel;
import com.kisquant.order.application.OrderRepository;
import com.kisquant.order.application.SequentialTradingIdGenerator;
import com.kisquant.order.domain.KisOrderNumber;
import com.kisquant.order.domain.KisOrderOrgNumber;
import com.kisquant.order.domain.Order;
import com.kisquant.order.domain.OrderQuantity;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ExecutionSyncApplicationServiceTest {

    @Test
    void pollingStoresOnlyDeltaFromCumulativeSnapshot() {
        Order acceptedOrder = liveAcceptedOrder(1L, StrategyId.of(1L), 3L);
        InMemoryExecutionRepository executions = new InMemoryExecutionRepository(
                execution(100L, acceptedOrder.id(), 1L, Money.won(1_000L), "already-1"));
        InMemoryPositionRepository positions = new InMemoryPositionRepository(
                StrategyPosition.restore(StrategyId.of(1L), Symbol.of("001510"), 1L, Money.won(1_000L), Money.ZERO));
        ExecutionSyncApplicationService service = service(
                new InMemoryOrderRepository(acceptedOrder),
                executions,
                positions,
                operationLogs(),
                order -> List.of(snapshot(order, 2L, Money.won(2_050L), Money.won(1_025L))));

        int synced = service.syncOpenOrders(10);

        assertThat(synced).isEqualTo(1);
        assertThat(executions.saved).singleElement().satisfies(execution -> {
            assertThat(execution.executedQuantity()).isEqualTo(1L);
            assertThat(execution.executedPrice()).isEqualTo(Money.won(1_050L));
            assertThat(execution.executedAmount()).isEqualTo(Money.won(1_050L));
        });
        assertThat(positions.findByStrategyIdAndSymbol(StrategyId.of(1L), Symbol.of("001510")))
                .hasValueSatisfying(position -> {
                    assertThat(position.quantity()).isEqualTo(2L);
                    assertThat(position.avgPrice()).isEqualTo(Money.won(1_025L));
                });
        assertThat(acceptedOrder.status()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
    }

    @Test
    void sameCumulativeSnapshotDoesNotCreateDuplicateExecution() {
        Order acceptedOrder = liveAcceptedOrder(1L, StrategyId.of(1L), 1L);
        InMemoryExecutionRepository executions = new InMemoryExecutionRepository(
                execution(100L, acceptedOrder.id(), 1L, Money.won(1_000L), "already-1"));
        InMemoryPositionRepository positions = new InMemoryPositionRepository(
                StrategyPosition.restore(StrategyId.of(1L), Symbol.of("001510"), 1L, Money.won(1_000L), Money.ZERO));
        ExecutionSyncApplicationService service = service(
                new InMemoryOrderRepository(acceptedOrder),
                executions,
                positions,
                operationLogs(),
                order -> List.of(snapshot(order, 1L, Money.won(1_000L), Money.won(1_000L))));

        int synced = service.syncOpenOrders(10);

        assertThat(synced).isZero();
        assertThat(executions.saved).isEmpty();
        assertThat(positions.findByStrategyIdAndSymbol(StrategyId.of(1L), Symbol.of("001510")))
                .hasValueSatisfying(position -> assertThat(position.quantity()).isEqualTo(1L));
        assertThat(acceptedOrder.status()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void pollingFailureIsLoggedAndDoesNotStopNextOrder() {
        Order failedOrder = liveAcceptedOrder(1L, StrategyId.of(1L), 1L);
        Order nextOrder = liveAcceptedOrder(2L, StrategyId.of(2L), 1L);
        InMemoryExecutionRepository executions = new InMemoryExecutionRepository();
        OperationLogBuffer operationLogs = operationLogs();
        ExecutionSyncApplicationService service = service(
                new InMemoryOrderRepository(failedOrder, nextOrder),
                executions,
                new InMemoryPositionRepository(),
                operationLogs,
                order -> {
                    if (order.id().equals(failedOrder.id())) {
                        throw new IllegalStateException("KIS request timeout");
                    }
                    return List.of(snapshot(order, 1L, Money.won(1_000L), Money.won(1_000L)));
                });

        int synced = service.syncOpenOrders(10);

        assertThat(synced).isEqualTo(1);
        assertThat(failedOrder.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(nextOrder.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(executions.saved).hasSize(1);
        assertThat(operationLogs.recent(null, 10)).singleElement().satisfies(log -> {
            assertThat(log.level()).isEqualTo(OperationLogLevel.ERROR);
            assertThat(log.category()).isEqualTo(OperationLogCategory.POLLING_ERROR);
            assertThat(log.message()).contains("LIVE 체결 동기화 실패", "orderId=1", "001510");
            assertThat(log.payloadJson()).contains("\"orderId\":1", "\"errorType\":\"IllegalStateException\"");
        });
    }

    private ExecutionSyncApplicationService service(
            InMemoryOrderRepository orders,
            InMemoryExecutionRepository executions,
            InMemoryPositionRepository positions,
            OperationLogBuffer operationLogs,
            KisExecutionGateway gateway
    ) {
        return new ExecutionSyncApplicationService(
                orders,
                executions,
                positions,
                operationLogs,
                gateway,
                new SequentialTradingIdGenerator(),
                () -> Instant.parse("2026-05-06T01:01:00Z"));
    }

    private OperationLogBuffer operationLogs() {
        return new OperationLogBuffer(() -> Instant.parse("2026-05-08T01:00:00Z"), 100);
    }

    private Order liveAcceptedOrder(long orderId, StrategyId strategyId, long quantity) {
        Order order = Order.requested(
                OrderId.of(orderId),
                strategyId,
                Symbol.of("001510"),
                OrderSide.BUY,
                TradeMode.LIVE,
                OrderType.LIMIT,
                OrderQuantity.of(quantity),
                Money.won(1_000L),
                Instant.parse("2026-05-06T01:00:00Z"));
        order.accept(KisOrderNumber.of("000710330" + orderId), KisOrderOrgNumber.of("06010"), Instant.parse("2026-05-06T01:00:01Z"));
        return order;
    }

    private Execution execution(long executionId, OrderId orderId, long quantity, Money price, String dedupKey) {
        return Execution.create(
                ExecutionId.of(executionId),
                orderId,
                StrategyId.of(1L),
                Symbol.of("001510"),
                OrderSide.BUY,
                ExecutionSource.LIVE,
                quantity,
                price,
                Instant.parse("2026-05-06T01:00:02Z"),
                ExecutionDedupKey.of(dedupKey));
    }

    private KisExecutionSnapshot snapshot(Order order, long cumulativeQuantity, Money cumulativeAmount, Money averagePrice) {
        return new KisExecutionSnapshot(
                order.kisOrderNumber().orElseThrow(),
                cumulativeQuantity,
                cumulativeAmount,
                averagePrice,
                Instant.parse("2026-05-06T01:00:03Z"));
    }

    private static final class InMemoryOrderRepository implements OrderRepository {
        private final List<Order> orders;

        private InMemoryOrderRepository(Order... orders) {
            this.orders = new ArrayList<>(List.of(orders));
        }

        @Override
        public Order save(Order order) {
            return order;
        }

        @Override
        public Optional<Order> findById(OrderId orderId) {
            return orders.stream().filter(order -> order.id().equals(orderId)).findFirst();
        }

        @Override
        public boolean existsOpenOrder(StrategyId strategyId, Symbol symbol) {
            return true;
        }

        @Override
        public List<Order> findPollingTargets(int batchSize) {
            return orders;
        }

        @Override
        public List<Order> findAll() {
            return orders;
        }
    }

    private static final class InMemoryExecutionRepository implements ExecutionRepository {
        private final List<Execution> existing = new ArrayList<>();
        private final List<Execution> saved = new ArrayList<>();

        private InMemoryExecutionRepository(Execution... existing) {
            this.existing.addAll(List.of(existing));
        }

        @Override
        public Execution save(Execution execution) {
            saved.add(execution);
            return execution;
        }

        @Override
        public boolean existsByDedupKey(ExecutionDedupKey dedupKey) {
            return findAll().stream().anyMatch(execution -> execution.dedupKey().equals(dedupKey));
        }

        @Override
        public List<Execution> findByOrderId(OrderId orderId) {
            return findAll().stream()
                    .filter(execution -> execution.orderId().equals(orderId))
                    .toList();
        }

        @Override
        public List<Execution> findAll() {
            List<Execution> all = new ArrayList<>(existing);
            all.addAll(saved);
            return all;
        }
    }

    private static final class InMemoryPositionRepository implements StrategyPositionRepository {
        private final Map<String, StrategyPosition> positions = new HashMap<>();

        private InMemoryPositionRepository(StrategyPosition... positions) {
            for (StrategyPosition position : positions) {
                this.positions.put(key(position.strategyId(), position.symbol()), position);
            }
        }

        @Override
        public StrategyPosition save(StrategyPosition position) {
            positions.put(key(position.strategyId(), position.symbol()), position);
            return position;
        }

        @Override
        public Optional<StrategyPosition> findByStrategyIdAndSymbol(StrategyId strategyId, Symbol symbol) {
            return Optional.ofNullable(positions.get(key(strategyId, symbol)));
        }

        @Override
        public List<StrategyPosition> findByStrategyId(StrategyId strategyId) {
            return positions.values().stream()
                    .filter(position -> position.strategyId().equals(strategyId))
                    .toList();
        }

        @Override
        public List<StrategyPosition> findAll() {
            return List.copyOf(positions.values());
        }

        @Override
        public void deleteByStrategyId(StrategyId strategyId) {
            positions.entrySet().removeIf(entry -> entry.getValue().strategyId().equals(strategyId));
        }

        private String key(StrategyId strategyId, Symbol symbol) {
            return strategyId.value() + ":" + symbol.value();
        }
    }

}
