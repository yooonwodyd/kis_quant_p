package com.kisquant.strategy.adapter.web;

import com.kisquant.execution.application.ExecutionQueryService;
import com.kisquant.order.application.OrderQueryService;
import com.kisquant.position.application.StrategyPositionApplicationService;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.strategylog.application.StrategyLogApplicationService;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin")
public class AdminReadApiController {

    private final OrderQueryService orderQueryService;
    private final ExecutionQueryService executionQueryService;
    private final StrategyPositionApplicationService positionService;
    private final StrategyLogApplicationService logService;

    public AdminReadApiController(
            OrderQueryService orderQueryService,
            ExecutionQueryService executionQueryService,
            StrategyPositionApplicationService positionService,
            StrategyLogApplicationService logService
    ) {
        this.orderQueryService = orderQueryService;
        this.executionQueryService = executionQueryService;
        this.positionService = positionService;
        this.logService = logService;
    }

    @GetMapping("/orders")
    public List<?> orders() {
        return orderQueryService.findOrders().stream()
                .map(order -> new AdminOrderResponse(
                        order.id().value(),
                        order.strategyId().value(),
                        order.symbol().value(),
                        order.side().name(),
                        order.tradeMode().name(),
                        order.orderType().name(),
                        order.quantity().value(),
                        order.orderPrice().amount(),
                        order.status().name()))
                .toList();
    }

    @GetMapping("/executions")
    public List<?> executions() {
        return executionQueryService.findExecutions().stream()
                .map(execution -> new AdminExecutionResponse(
                        execution.id().value(),
                        execution.orderId().value(),
                        execution.strategyId().value(),
                        execution.symbol().value(),
                        execution.side().name(),
                        execution.source().name(),
                        execution.executedQuantity(),
                        execution.executedPrice().amount(),
                        execution.executedAmount().amount(),
                        execution.executedAt()))
                .toList();
    }

    @GetMapping("/positions")
    public List<StrategyPositionResponse> positions() {
        return positionService.findPositions().stream()
                .map(position -> new StrategyPositionResponse(
                        position.strategyId().value(),
                        position.symbol().value(),
                        position.quantity(),
                        position.avgPrice().amount(),
                        position.realizedPnl().amount()))
                .toList();
    }

    @GetMapping("/strategy-logs")
    public List<StrategyLogResponse> strategyLogs(
            @RequestParam(required = false) @Min(1L) Long strategyId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        List<com.kisquant.strategylog.domain.StrategyLog> logs = strategyId == null
                ? logService.recentLogs(limit)
                : logService.recentLogs(StrategyId.of(strategyId), limit);
        return logs.stream()
                .map(log -> new StrategyLogResponse(
                        log.strategyId().value(),
                        log.level().name(),
                        log.message(),
                        log.payloadJson(),
                        log.createdAt()))
                .toList();
    }

    public record AdminOrderResponse(
            long orderId,
            long strategyId,
            String symbol,
            String side,
            String tradeMode,
            String orderType,
            long quantity,
            long orderPrice,
            String status
    ) {
    }

    public record AdminExecutionResponse(
            long executionId,
            long orderId,
            long strategyId,
            String symbol,
            String side,
            String source,
            long executedQuantity,
            long executedPrice,
            long executedAmount,
            Instant executedAt
    ) {
    }
}
