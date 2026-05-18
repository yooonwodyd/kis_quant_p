package com.kisquant.strategy.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kisquant.order.application.PlaceOrderApplicationService;
import com.kisquant.order.application.PlaceOrderResult;
import com.kisquant.order.domain.OrderStatus;
import com.kisquant.position.application.StrategyPositionApplicationService;
import com.kisquant.position.domain.StrategyPosition;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.shared.error.GlobalExceptionHandler;
import com.kisquant.strategylog.application.StrategyLogApplicationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class StrategyApiControllerTest {

    @Test
    void placeOrderReturnsResponseDto() {
        PlaceOrderApplicationService orders = mock(PlaceOrderApplicationService.class);
        when(orders.placeOrder(any())).thenReturn(new PlaceOrderResult(OrderId.of(1L), OrderStatus.FILLED, null, "simulation filled"));
        StrategyApiController controller = new StrategyApiController(
                orders,
                mock(StrategyPositionApplicationService.class),
                mock(StrategyLogApplicationService.class));
        RestTestClient client = RestTestClient.bindToController(controller)
                .configureServer(server -> server.setControllerAdvice(new GlobalExceptionHandler()))
                .build();

        client.post()
                .uri("/api/strategy/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "strategyId": 1,
                          "symbol": "001510",
                          "side": "BUY",
                          "orderType": "LIMIT",
                          "quantity": 1,
                          "orderPrice": 1000
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orderId").isEqualTo(1)
                .jsonPath("$.status").isEqualTo("FILLED");
    }

    @Test
    void invalidOrderRequestReturnsCommonErrorResponse() {
        StrategyApiController controller = new StrategyApiController(
                mock(PlaceOrderApplicationService.class),
                mock(StrategyPositionApplicationService.class),
                mock(StrategyLogApplicationService.class));
        RestTestClient client = RestTestClient.bindToController(controller)
                .configureServer(server -> server.setControllerAdvice(new GlobalExceptionHandler()))
                .build();

        client.post()
                .uri("/api/strategy/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "strategyId": 1,
                          "symbol": "001510",
                          "side": "BUY",
                          "orderType": "LIMIT",
                          "quantity": 0,
                          "orderPrice": 1000
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void positionsReturnsAllPositionsForStrategyWhenSymbolIsNotSpecified() {
        StrategyPositionApplicationService positions = mock(StrategyPositionApplicationService.class);
        when(positions.findPositions(StrategyId.of(1L))).thenReturn(List.of(
                StrategyPosition.restore(StrategyId.of(1L), Symbol.of("005930"), 2L, Money.won(70_000L), Money.ZERO),
                StrategyPosition.restore(StrategyId.of(1L), Symbol.of("000660"), 1L, Money.won(120_000L), Money.won(5_000L))));
        StrategyApiController controller = new StrategyApiController(
                mock(PlaceOrderApplicationService.class),
                positions,
                mock(StrategyLogApplicationService.class));
        RestTestClient client = RestTestClient.bindToController(controller)
                .configureServer(server -> server.setControllerAdvice(new GlobalExceptionHandler()))
                .build();

        client.get()
                .uri("/api/strategy/positions?strategyId=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].strategyId").isEqualTo(1)
                .jsonPath("$[0].symbol").isEqualTo("005930")
                .jsonPath("$[0].quantity").isEqualTo(2)
                .jsonPath("$[0].avgPrice").isEqualTo(70000)
                .jsonPath("$[1].symbol").isEqualTo("000660")
                .jsonPath("$[1].realizedPnl").isEqualTo(5000);
    }
}
