package com.kisquant.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kisquant.execution.adapter.persistence.JpaExecutionRepositoryAdapter;
import com.kisquant.execution.domain.Execution;
import com.kisquant.execution.domain.ExecutionDedupKey;
import com.kisquant.execution.domain.ExecutionId;
import com.kisquant.execution.domain.ExecutionSource;
import com.kisquant.order.adapter.persistence.JpaTradingIdGenerator;
import com.kisquant.order.adapter.persistence.JpaOrderRepositoryAdapter;
import com.kisquant.order.domain.KisOrderNumber;
import com.kisquant.order.domain.KisOrderOrgNumber;
import com.kisquant.order.domain.Order;
import com.kisquant.order.domain.OrderQuantity;
import com.kisquant.order.domain.OrderSide;
import com.kisquant.order.domain.OrderStatus;
import com.kisquant.order.domain.OrderType;
import com.kisquant.position.adapter.persistence.JpaStrategyPositionRepositoryAdapter;
import com.kisquant.position.domain.StrategyPosition;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.shared.domain.TradeMode;
import com.kisquant.strategy.adapter.persistence.JpaStrategyIdGenerator;
import com.kisquant.strategy.adapter.persistence.JpaStrategyRepositoryAdapter;
import com.kisquant.strategy.domain.Strategy;
import com.kisquant.strategylog.adapter.persistence.JpaStrategyLogRepositoryAdapter;
import com.kisquant.strategylog.domain.LogLevel;
import com.kisquant.strategylog.domain.StrategyLog;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaStrategyRepositoryAdapter.class,
        JpaStrategyIdGenerator.class,
        JpaOrderRepositoryAdapter.class,
        JpaExecutionRepositoryAdapter.class,
        JpaStrategyPositionRepositoryAdapter.class,
        JpaStrategyLogRepositoryAdapter.class
})
class JpaPersistenceAdapterTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    JpaStrategyRepositoryAdapter strategyRepository;

    @Autowired
    JpaStrategyIdGenerator strategyIdGenerator;

    @Autowired
    JpaOrderRepositoryAdapter orderRepository;

    @Autowired
    JpaExecutionRepositoryAdapter executionRepository;

    @Autowired
    JpaStrategyPositionRepositoryAdapter positionRepository;

    @Autowired
    JpaStrategyLogRepositoryAdapter logRepository;

    @Test
    void flywayCreatesCoreTables() {
        Set<String> tableNames = Set.copyOf(jdbcTemplate.queryForList("SHOW TABLES", String.class));

        assertThat(tableNames)
                .contains("strategies", "orders", "executions", "strategy_positions", "strategy_logs");
    }

    @Test
    void flywaySeedsManualSimulationAndLiveStrategies() {
        assertThat(strategyRepository.findById(StrategyId.of(1L)))
                .hasValueSatisfying(strategy -> {
                    assertThat(strategy.name()).isEqualTo("수동 모의 투자 전략");
                    assertThat(strategy.tradeMode()).isEqualTo(TradeMode.SIMULATION);
                    assertThat(strategy.initialBudget()).isEqualTo(Money.won(30_000L));
                });
        assertThat(strategyRepository.findById(StrategyId.of(2L)))
                .hasValueSatisfying(strategy -> {
                    assertThat(strategy.name()).isEqualTo("수동 실전 투자 전략");
                    assertThat(strategy.tradeMode()).isEqualTo(TradeMode.LIVE);
                    assertThat(strategy.initialBudget()).isEqualTo(Money.won(30_000L));
                });
    }

    @Test
    void strategyRepositoryRoundTrip() {
        Strategy strategy = Strategy.create(
                StrategyId.of(99L),
                "수동 주문 테스트 전략",
                TradeMode.LIVE,
                Money.won(30_000L),
                Money.won(30_000L),
                Money.won(30_000L));

        strategyRepository.save(strategy);

        assertThat(strategyRepository.findById(StrategyId.of(99L)))
                .hasValueSatisfying(found -> {
                    assertThat(found.name()).isEqualTo("수동 주문 테스트 전략");
                    assertThat(found.tradeMode()).isEqualTo(TradeMode.LIVE);
                    assertThat(found.initialBudget()).isEqualTo(Money.won(30_000L));
                });
    }

    @Test
    void strategyRepositoryUpdatesSeededStrategyWithoutLosingTimestamps() {
        Strategy strategy = strategyRepository.findById(StrategyId.of(1L)).orElseThrow();
        strategy.changeInitialBudget(Money.won(40_000L));

        strategyRepository.save(strategy);

        assertThat(strategyRepository.findById(StrategyId.of(1L)))
                .hasValueSatisfying(found -> assertThat(found.initialBudget()).isEqualTo(Money.won(40_000L)));
    }

    @Test
    void strategyIdGeneratorStartsAfterSeededStrategies() {
        assertThat(strategyIdGenerator.nextStrategyId()).isEqualTo(StrategyId.of(3L));
    }

    @Test
    void orderRepositoryFindsOpenOrder() {
        Order order = Order.requested(
                OrderId.of(1L),
                StrategyId.of(1L),
                Symbol.of("001510"),
                OrderSide.BUY,
                TradeMode.LIVE,
                OrderType.LIMIT,
                OrderQuantity.of(1L),
                Money.won(1_000L),
                Instant.parse("2026-05-06T01:00:00Z"));

        orderRepository.save(order);

        assertThat(orderRepository.existsOpenOrder(StrategyId.of(1L), Symbol.of("001510"))).isTrue();
    }

    @Test
    void orderRepositoryUpdatesExistingOrderWithoutLosingTimestamps() {
        Order order = Order.requested(
                OrderId.of(11L),
                StrategyId.of(1L),
                Symbol.of("001510"),
                OrderSide.BUY,
                TradeMode.LIVE,
                OrderType.LIMIT,
                OrderQuantity.of(1L),
                Money.won(1_000L),
                Instant.parse("2026-05-06T01:00:00Z"));
        orderRepository.save(order);

        order.accept(KisOrderNumber.of("0007103300"), KisOrderOrgNumber.of("06010"), Instant.parse("2026-05-06T01:00:01Z"));
        orderRepository.save(order);

        assertThat(orderRepository.findById(OrderId.of(11L)))
                .hasValueSatisfying(found -> assertThat(found.status()).isEqualTo(OrderStatus.ACCEPTED));
    }

    @Test
    void executionRepositoryUsesDedupKeyAsUniqueConstraint() {
        Execution execution = execution(ExecutionId.of(1L), ExecutionDedupKey.of("live-1"));

        executionRepository.save(execution);

        assertThatThrownBy(() -> executionRepository.save(execution(ExecutionId.of(2L), ExecutionDedupKey.of("live-1"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void executionRepositoryFindsByOrderId() {
        executionRepository.save(execution(ExecutionId.of(1L), OrderId.of(1L), ExecutionDedupKey.of("order-1")));
        executionRepository.save(execution(ExecutionId.of(2L), OrderId.of(2L), ExecutionDedupKey.of("order-2")));

        assertThat(executionRepository.findByOrderId(OrderId.of(1L)))
                .singleElement()
                .satisfies(found -> assertThat(found.dedupKey()).isEqualTo(ExecutionDedupKey.of("order-1")));
    }

    @Test
    void tradingIdGeneratorStartsAfterPersistedMaxIds() {
        orderRepository.save(Order.requested(
                OrderId.of(30L),
                StrategyId.of(1L),
                Symbol.of("001510"),
                OrderSide.BUY,
                TradeMode.SIMULATION,
                OrderType.MARKET,
                OrderQuantity.of(1L),
                Money.won(1_000L),
                Instant.parse("2026-05-06T01:00:00Z")));
        executionRepository.save(execution(ExecutionId.of(40L), ExecutionDedupKey.of("generator-40")));

        JpaTradingIdGenerator idGenerator = new JpaTradingIdGenerator(jdbcTemplate);

        assertThat(idGenerator.nextOrderId()).isEqualTo(OrderId.of(31L));
        assertThat(idGenerator.nextExecutionId()).isEqualTo(ExecutionId.of(41L));
    }


    @Test
    void strategyPositionRepositoryRoundTrip() {
        StrategyPosition position = StrategyPosition.empty(StrategyId.of(1L), Symbol.of("001510"));
        position.applyExecution(execution(ExecutionId.of(1L), ExecutionDedupKey.of("position-1")));

        positionRepository.save(position);

        assertThat(positionRepository.findByStrategyIdAndSymbol(StrategyId.of(1L), Symbol.of("001510")))
                .hasValueSatisfying(found -> {
                    assertThat(found.quantity()).isEqualTo(1L);
                    assertThat(found.avgPrice()).isEqualTo(Money.won(1_000L));
                });
    }

    @Test
    void strategyPositionRepositoryDeletesByStrategyId() {
        StrategyPosition target = StrategyPosition.restore(
                StrategyId.of(1L),
                Symbol.of("001510"),
                1L,
                Money.won(1_000L),
                Money.ZERO);
        StrategyPosition other = StrategyPosition.restore(
                StrategyId.of(2L),
                Symbol.of("001510"),
                1L,
                Money.won(1_000L),
                Money.ZERO);
        positionRepository.save(target);
        positionRepository.save(other);

        positionRepository.deleteByStrategyId(StrategyId.of(1L));

        assertThat(positionRepository.findByStrategyIdAndSymbol(StrategyId.of(1L), Symbol.of("001510"))).isEmpty();
        assertThat(positionRepository.findByStrategyIdAndSymbol(StrategyId.of(2L), Symbol.of("001510"))).isPresent();
    }

    @Test
    void strategyLogRepositoryReturnsRecentLogs() {
        StrategyLog log = StrategyLog.create(
                StrategyId.of(1L),
                LogLevel.INFO,
                "SIMULATION 주문 체결: 001510 1주 1000원",
                "{\"mode\":\"SIMULATION\"}",
                Instant.parse("2026-05-06T01:00:00Z"));

        logRepository.save(log);

        assertThat(logRepository.findRecentByStrategyId(StrategyId.of(1L), 10))
                .singleElement()
                .satisfies(found -> assertThat(found.message()).contains("SIMULATION 주문 체결"));
    }

    @Test
    void strategyLogRepositoryReturnsRecentLogsAcrossAllStrategies() {
        logRepository.save(StrategyLog.create(
                StrategyId.of(1L),
                LogLevel.INFO,
                "SIMULATION 주문 체결",
                null,
                Instant.parse("2026-05-06T01:00:00Z")));
        logRepository.save(StrategyLog.create(
                StrategyId.of(2L),
                LogLevel.INFO,
                "LIVE 주문 접수",
                null,
                Instant.parse("2026-05-06T01:01:00Z")));

        assertThat(logRepository.findRecent(10))
                .extracting(log -> log.strategyId().value())
                .containsExactly(2L, 1L);
    }

    private Execution execution(ExecutionId id, ExecutionDedupKey dedupKey) {
        return execution(id, OrderId.of(1L), dedupKey);
    }

    private Execution execution(ExecutionId id, OrderId orderId, ExecutionDedupKey dedupKey) {
        return Execution.create(
                id,
                orderId,
                StrategyId.of(1L),
                Symbol.of("001510"),
                OrderSide.BUY,
                ExecutionSource.LIVE,
                1L,
                Money.won(1_000L),
                Instant.parse("2026-05-06T01:00:00Z"),
                dedupKey);
    }
}
