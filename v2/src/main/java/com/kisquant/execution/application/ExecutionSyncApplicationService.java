package com.kisquant.execution.application;

import com.kisquant.execution.domain.Execution;
import com.kisquant.execution.domain.ExecutionDedupKey;
import com.kisquant.execution.domain.ExecutionSource;
import com.kisquant.operationlog.application.OperationLogBuffer;
import com.kisquant.operationlog.domain.OperationLogCategory;
import com.kisquant.operationlog.domain.OperationLogLevel;
import com.kisquant.order.application.OrderRepository;
import com.kisquant.order.application.TradingIdGenerator;
import com.kisquant.order.domain.Order;
import com.kisquant.position.application.StrategyPositionRepository;
import com.kisquant.position.domain.PositionUpdateService;
import com.kisquant.position.domain.StrategyPosition;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.time.CurrentTimeProvider;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutionSyncApplicationService implements OpenOrderSyncUseCase {

    private final OrderRepository orderRepository;
    private final ExecutionRepository executionRepository;
    private final StrategyPositionRepository positionRepository;
    private final OperationLogBuffer operationLogs;
    private final KisExecutionGateway kisExecutionGateway;
    private final TradingIdGenerator idGenerator;
    private final CurrentTimeProvider timeProvider;
    private final PositionUpdateService positionUpdateService = new PositionUpdateService();

    public ExecutionSyncApplicationService(
            OrderRepository orderRepository,
            ExecutionRepository executionRepository,
            StrategyPositionRepository positionRepository,
            OperationLogBuffer operationLogs,
            KisExecutionGateway kisExecutionGateway,
            TradingIdGenerator idGenerator,
            CurrentTimeProvider timeProvider
    ) {
        this.orderRepository = orderRepository;
        this.executionRepository = executionRepository;
        this.positionRepository = positionRepository;
        this.operationLogs = operationLogs;
        this.kisExecutionGateway = kisExecutionGateway;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    @Transactional
    @Override
    public int syncOpenOrders(int batchSize) {
        int synced = 0;
        List<Order> orders = orderRepository.findPollingTargets(batchSize);
        for (Order order : orders) {
            synced += syncOrder(order);
        }
        return synced;
    }

    private int syncOrder(Order order) {
        int synced = 0;
        List<KisExecutionSnapshot> snapshots;
        try {
            snapshots = kisExecutionGateway.findExecutionSnapshots(order);
        } catch (RuntimeException exception) {
            savePollingFailureLog(order, exception);
            return 0;
        }

        List<Execution> appliedExecutions = executionRepository.findByOrderId(order.id());
        long appliedQuantity = appliedExecutions.stream().mapToLong(Execution::executedQuantity).sum();
        Money appliedAmount = appliedExecutions.stream()
                .map(Execution::executedAmount)
                .reduce(Money.ZERO, Money::plus);
        long maxCumulativeQuantity = 0L;

        for (KisExecutionSnapshot snapshot : snapshots.stream()
                .sorted(Comparator.comparingLong(KisExecutionSnapshot::cumulativeExecutedQuantity))
                .toList()) {
            maxCumulativeQuantity = Math.max(maxCumulativeQuantity, snapshot.cumulativeExecutedQuantity());
            long deltaQuantity = snapshot.cumulativeExecutedQuantity() - appliedQuantity;
            Money deltaAmount = snapshot.cumulativeExecutedAmount().minus(appliedAmount);
            if (deltaQuantity <= 0L || deltaAmount.compareTo(Money.ZERO) <= 0) {
                continue;
            }

            Money deltaPrice = Money.won(Math.floorDiv(deltaAmount.amount(), deltaQuantity));
            Execution execution = Execution.createWithAmount(
                    idGenerator.nextExecutionId(),
                    order.id(),
                    order.strategyId(),
                    order.symbol(),
                    order.side(),
                    ExecutionSource.LIVE,
                    deltaQuantity,
                    deltaPrice,
                    deltaAmount,
                    snapshot.executedAt(),
                    ExecutionDedupKey.of("LIVE-" + snapshot.kisOrderNumber().value() + "-"
                            + snapshot.cumulativeExecutedQuantity() + "-"
                            + snapshot.cumulativeExecutedAmount().amount()));
            executionRepository.save(execution);
            StrategyPosition position = positionRepository
                    .findByStrategyIdAndSymbol(execution.strategyId(), execution.symbol())
                    .orElseGet(() -> StrategyPosition.empty(execution.strategyId(), execution.symbol()));
            positionUpdateService.apply(position, execution);
            positionRepository.save(position);
            appliedQuantity = snapshot.cumulativeExecutedQuantity();
            appliedAmount = snapshot.cumulativeExecutedAmount();
            synced++;
        }
        if (maxCumulativeQuantity > 0L) {
            Instant now = timeProvider.now();
            order.applyExecution(maxCumulativeQuantity, now);
            orderRepository.save(order);
        }
        return synced;
    }

    private void savePollingFailureLog(Order order, RuntimeException exception) {
        String message = "LIVE 체결 동기화 실패: orderId=" + order.id().value()
                + " symbol=" + order.symbol().value()
                + " reason=" + exception.getMessage();
        String payload = "{\"orderId\":" + order.id().value()
                + ",\"symbol\":\"" + jsonEscape(order.symbol().value()) + "\""
                + ",\"kisOrderNumber\":\"" + jsonEscape(order.kisOrderNumber().map(number -> number.value()).orElse("")) + "\""
                + ",\"status\":\"" + order.status().name() + "\""
                + ",\"errorType\":\"" + jsonEscape(exception.getClass().getSimpleName()) + "\""
                + ",\"message\":\"" + jsonEscape(exception.getMessage()) + "\"}";
        operationLogs.append(OperationLogLevel.ERROR, OperationLogCategory.POLLING_ERROR, message, payload);
    }

    private String jsonEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
