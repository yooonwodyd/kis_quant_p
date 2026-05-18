package com.kisquant.strategy.adapter.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kisquant.execution.application.ExecutionQueryService;
import com.kisquant.execution.domain.Execution;
import com.kisquant.execution.domain.ExecutionDedupKey;
import com.kisquant.execution.domain.ExecutionId;
import com.kisquant.execution.domain.ExecutionSource;
import com.kisquant.order.application.OrderQueryService;
import com.kisquant.order.domain.OrderSide;
import com.kisquant.position.application.StrategyPositionApplicationService;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.strategylog.application.StrategyLogApplicationService;
import com.kisquant.strategylog.domain.LogLevel;
import com.kisquant.strategylog.domain.StrategyLog;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.client.RestTestClient;

class AdminReadApiControllerTest {

    @Test
    void executionsIncludeExecutedAtForDashboardTradeHistory() {
        ExecutionQueryService executions = mock(ExecutionQueryService.class);
        when(executions.findExecutions()).thenReturn(List.of(new Execution(
                ExecutionId.of(1L),
                OrderId.of(10L),
                StrategyId.of(2L),
                Symbol.of("001510"),
                OrderSide.BUY,
                ExecutionSource.SIMULATION,
                3L,
                Money.won(5_570L),
                Money.won(16_710L),
                Instant.parse("2026-05-11T00:30:00Z"),
                ExecutionDedupKey.of("SIM-1"))));
        AdminReadApiController controller = new AdminReadApiController(
                mock(OrderQueryService.class),
                executions,
                mock(StrategyPositionApplicationService.class),
                mock(StrategyLogApplicationService.class));
        RestTestClient client = RestTestClient.bindToController(controller).build();

        client.get()
                .uri("/api/admin/executions")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].executedAt").isEqualTo("2026-05-11T00:30:00Z")
                .jsonPath("$[0].executedPrice").isEqualTo(5570)
                .jsonPath("$[0].executedQuantity").isEqualTo(3);
    }

    @Test
    void strategyLogsCanReturnAllStrategiesWhenStrategyIdIsNotSpecified() {
        StrategyLogApplicationService logs = mock(StrategyLogApplicationService.class);
        when(logs.recentLogs(50)).thenReturn(List.of(
                StrategyLog.create(
                        StrategyId.of(1L),
                        LogLevel.INFO,
                        "SIMULATION 주문 체결",
                        null,
                        Instant.parse("2026-05-11T00:00:00Z")),
                StrategyLog.create(
                        StrategyId.of(2L),
                        LogLevel.INFO,
                        "LIVE 주문 접수",
                        null,
                        Instant.parse("2026-05-11T00:01:00Z"))));
        AdminReadApiController controller = new AdminReadApiController(
                mock(OrderQueryService.class),
                mock(ExecutionQueryService.class),
                mock(StrategyPositionApplicationService.class),
                logs);
        RestTestClient client = RestTestClient.bindToController(controller).build();

        client.get()
                .uri("/api/admin/strategy-logs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].strategyId").isEqualTo(1)
                .jsonPath("$[1].strategyId").isEqualTo(2);
    }
}
