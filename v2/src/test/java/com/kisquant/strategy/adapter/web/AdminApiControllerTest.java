package com.kisquant.strategy.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.TradeMode;
import com.kisquant.strategy.application.AdminStrategyApplicationService;
import com.kisquant.strategy.application.CreateStrategyCommand;
import com.kisquant.strategy.domain.Strategy;
import com.kisquant.shared.error.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class AdminApiControllerTest {

    @Test
    void createStrategyReturnsCreatedStrategyId() {
        AdminStrategyApplicationService service = mock(AdminStrategyApplicationService.class);
        Strategy strategy = Strategy.create(StrategyId.of(3L), "이동평균 Python 전략", TradeMode.SIMULATION,
                Money.won(30_000L), Money.won(10_000L), Money.won(30_000L));
        when(service.createStrategy(any(CreateStrategyCommand.class))).thenReturn(strategy);
        AdminApiController controller = new AdminApiController(service);
        RestTestClient client = RestTestClient.bindToController(controller)
                .configureServer(server -> server.setControllerAdvice(new GlobalExceptionHandler()))
                .build();

        client.post()
                .uri("/api/admin/strategies")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "name": "이동평균 Python 전략",
                          "tradeMode": "SIMULATION",
                          "initialBudgetAmount": 30000,
                          "maxOrderAmount": 10000,
                          "maxDailyOrderAmount": 30000
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(3)
                .jsonPath("$.name").isEqualTo("이동평균 Python 전략")
                .jsonPath("$.tradeMode").isEqualTo("SIMULATION");
    }

    @Test
    void strategiesReturnsAdminStrategyDtos() {
        AdminStrategyApplicationService service = mock(AdminStrategyApplicationService.class);
        when(service.findStrategies()).thenReturn(List.of(strategy()));
        AdminApiController controller = new AdminApiController(service);
        RestTestClient client = RestTestClient.bindToController(controller)
                .configureServer(server -> server.setControllerAdvice(new GlobalExceptionHandler()))
                .build();

        client.get()
                .uri("/api/admin/strategies")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(1)
                .jsonPath("$[0].tradeMode").isEqualTo("SIMULATION")
                .jsonPath("$[0].initialBudgetAmount").isEqualTo(30000);
    }

    @Test
    void statusPatchChangesEnabledFlag() {
        AdminStrategyApplicationService service = mock(AdminStrategyApplicationService.class);
        Strategy strategy = strategy();
        strategy.deactivate();
        when(service.changeEnabled(StrategyId.of(1L), false)).thenReturn(strategy);
        AdminApiController controller = new AdminApiController(service);
        RestTestClient client = RestTestClient.bindToController(controller)
                .configureServer(server -> server.setControllerAdvice(new GlobalExceptionHandler()))
                .build();

        client.patch()
                .uri("/api/admin/strategies/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"enabled\":false}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.enabled").isEqualTo(false);
    }

    @Test
    void budgetPatchChangesInitialBudget() {
        AdminStrategyApplicationService service = mock(AdminStrategyApplicationService.class);
        Strategy strategy = Strategy.create(StrategyId.of(1L), "수동 주문 테스트 전략", TradeMode.SIMULATION,
                Money.won(40_000L), Money.won(30_000L), Money.won(30_000L));
        when(service.changeInitialBudget(eq(StrategyId.of(1L)), eq(Money.won(40_000L)))).thenReturn(strategy);
        AdminApiController controller = new AdminApiController(service);
        RestTestClient client = RestTestClient.bindToController(controller)
                .configureServer(server -> server.setControllerAdvice(new GlobalExceptionHandler()))
                .build();

        client.patch()
                .uri("/api/admin/strategies/1/budget")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"initialBudgetAmount\":40000}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.initialBudgetAmount").isEqualTo(40000);
    }

    @Test
    void resetPositionsDeletesStrategyPositions() {
        AdminStrategyApplicationService service = mock(AdminStrategyApplicationService.class);
        AdminApiController controller = new AdminApiController(service);
        RestTestClient client = RestTestClient.bindToController(controller)
                .configureServer(server -> server.setControllerAdvice(new GlobalExceptionHandler()))
                .build();

        client.delete()
                .uri("/api/admin/strategies/1/positions")
                .exchange()
                .expectStatus().isNoContent();

        verify(service).resetPositions(StrategyId.of(1L));
    }

    private Strategy strategy() {
        return Strategy.create(StrategyId.of(1L), "수동 주문 테스트 전략", TradeMode.SIMULATION,
                Money.won(30_000L), Money.won(30_000L), Money.won(30_000L));
    }
}
