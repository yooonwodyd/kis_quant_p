package com.kisquant.strategy.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kisquant.position.application.PositionResetReason;
import com.kisquant.position.application.StrategyPositionResetter;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.TradeMode;
import com.kisquant.strategy.domain.Strategy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdminStrategyApplicationServiceTest {

    @Test
    void createStrategyAssignsNextIdAndSavesEnabledStrategy() {
        InMemoryStrategyRepository repository = new InMemoryStrategyRepository();
        AdminStrategyApplicationService service = new AdminStrategyApplicationService(
                repository,
                new RecordingPositionResetter(),
                () -> StrategyId.of(3L));

        Strategy created = service.createStrategy(new CreateStrategyCommand(
                "이동평균 Python 전략",
                TradeMode.SIMULATION,
                Money.won(30_000L),
                Money.won(10_000L),
                Money.won(30_000L)));

        assertThat(created.id()).isEqualTo(StrategyId.of(3L));
        assertThat(created.name()).isEqualTo("이동평균 Python 전략");
        assertThat(created.enabled()).isTrue();
        assertThat(repository.findById(StrategyId.of(3L))).hasValue(created);
    }

    @Test
    void changingTradeModeResetsStrategyPositions() {
        RecordingPositionResetter positionResetter = new RecordingPositionResetter();
        AdminStrategyApplicationService service = new AdminStrategyApplicationService(
                new InMemoryStrategyRepository(simulationStrategy()),
                positionResetter,
                () -> StrategyId.of(99L));

        service.changeTradeMode(StrategyId.of(1L), TradeMode.LIVE);

        assertThat(positionResetter.calls).containsExactly(new ResetCall(StrategyId.of(1L), PositionResetReason.TRADE_MODE_CHANGE));
    }

    @Test
    void keepingSameTradeModeDoesNotResetPositions() {
        RecordingPositionResetter positionResetter = new RecordingPositionResetter();
        AdminStrategyApplicationService service = new AdminStrategyApplicationService(
                new InMemoryStrategyRepository(simulationStrategy()),
                positionResetter,
                () -> StrategyId.of(99L));

        service.changeTradeMode(StrategyId.of(1L), TradeMode.SIMULATION);

        assertThat(positionResetter.calls).isEmpty();
    }

    @Test
    void resetPositionsDelegatesManualPositionReset() {
        RecordingPositionResetter positionResetter = new RecordingPositionResetter();
        AdminStrategyApplicationService service = new AdminStrategyApplicationService(
                new InMemoryStrategyRepository(simulationStrategy()),
                positionResetter,
                () -> StrategyId.of(99L));

        service.resetPositions(StrategyId.of(1L));

        assertThat(positionResetter.calls).containsExactly(new ResetCall(StrategyId.of(1L), PositionResetReason.MANUAL));
    }

    @Test
    void resetPositionsPropagatesPositionResetFailure() {
        RecordingPositionResetter positionResetter = new RecordingPositionResetter();
        positionResetter.failOnReset = true;
        AdminStrategyApplicationService service = new AdminStrategyApplicationService(
                new InMemoryStrategyRepository(simulationStrategy()),
                positionResetter,
                () -> StrategyId.of(99L));

        assertThatThrownBy(() -> service.resetPositions(StrategyId.of(1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("reset failed");
    }

    private Strategy simulationStrategy() {
        return Strategy.create(StrategyId.of(1L), "수동 모의 투자 전략", TradeMode.SIMULATION,
                Money.won(30_000L), Money.won(30_000L), Money.won(30_000L));
    }

    private static final class InMemoryStrategyRepository implements StrategyRepository {
        private final Map<StrategyId, Strategy> strategies = new HashMap<>();

        private InMemoryStrategyRepository() {
        }

        private InMemoryStrategyRepository(Strategy strategy) {
            strategies.put(strategy.id(), strategy);
        }

        @Override
        public Strategy save(Strategy strategy) {
            strategies.put(strategy.id(), strategy);
            return strategy;
        }

        @Override
        public Optional<Strategy> findById(StrategyId strategyId) {
            return Optional.ofNullable(strategies.get(strategyId));
        }

        @Override
        public List<Strategy> findAll() {
            return List.copyOf(strategies.values());
        }
    }

    private record ResetCall(StrategyId strategyId, PositionResetReason reason) {
    }

    private static final class RecordingPositionResetter implements StrategyPositionResetter {
        private final List<ResetCall> calls = new ArrayList<>();
        private boolean failOnReset;

        @Override
        public void resetPositions(StrategyId strategyId, PositionResetReason reason) {
            if (failOnReset) {
                throw new IllegalStateException("reset failed");
            }
            calls.add(new ResetCall(strategyId, reason));
        }
    }
}
