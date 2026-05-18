package com.kisquant.strategy.adapter.web;

import com.kisquant.order.application.PlaceOrderApplicationService;
import com.kisquant.order.application.PlaceOrderCommand;
import com.kisquant.order.application.PlaceOrderResult;
import com.kisquant.position.application.StrategyPositionApplicationService;
import com.kisquant.position.domain.StrategyPosition;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.strategylog.application.CreateStrategyLogCommand;
import com.kisquant.strategylog.application.StrategyLogApplicationService;
import com.kisquant.strategylog.domain.StrategyLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/strategy")
public class StrategyApiController {

    private final PlaceOrderApplicationService placeOrderService;
    private final StrategyPositionApplicationService positionService;
    private final StrategyLogApplicationService logService;

    public StrategyApiController(
            PlaceOrderApplicationService placeOrderService,
            StrategyPositionApplicationService positionService,
            StrategyLogApplicationService logService
    ) {
        this.placeOrderService = placeOrderService;
        this.positionService = positionService;
        this.logService = logService;
    }

    @PostMapping("/orders")
    public OrderResponse placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        PlaceOrderResult result = placeOrderService.placeOrder(new PlaceOrderCommand(
                StrategyId.of(request.strategyId()),
                Symbol.of(request.symbol()),
                request.side(),
                request.orderType(),
                request.quantity(),
                Money.won(request.orderPrice())));
        return new OrderResponse(
                result.orderId().value(),
                result.status().name(),
                result.kisOrderNumber().orElse(null),
                result.message());
    }

    @GetMapping(value = "/positions", params = "symbol")
    public StrategyPositionResponse position(
            @RequestParam @Min(1L) long strategyId,
            @RequestParam @Pattern(regexp = "\\d{6}") String symbol
    ) {
        StrategyPosition position = positionService.findPosition(StrategyId.of(strategyId), Symbol.of(symbol))
                .orElseGet(() -> StrategyPosition.empty(StrategyId.of(strategyId), Symbol.of(symbol)));
        return toPositionResponse(position);
    }

    @GetMapping(value = "/positions", params = "!symbol")
    public List<StrategyPositionResponse> positions(@RequestParam @Min(1L) long strategyId) {
        return positionService.findPositions(StrategyId.of(strategyId)).stream()
                .map(this::toPositionResponse)
                .toList();
    }

    @PostMapping("/logs")
    public StrategyLogResponse createLog(@Valid @RequestBody CreateStrategyLogRequest request) {
        StrategyLog log = logService.createLog(new CreateStrategyLogCommand(
                StrategyId.of(request.strategyId()),
                request.level(),
                request.message(),
                request.payloadJson()));
        return toLogResponse(log);
    }

    @GetMapping("/logs")
    public List<StrategyLogResponse> logs(@RequestParam @Min(1L) long strategyId, @RequestParam(defaultValue = "50") int limit) {
        return logService.recentLogs(StrategyId.of(strategyId), limit).stream()
                .map(this::toLogResponse)
                .toList();
    }

    private StrategyPositionResponse toPositionResponse(StrategyPosition position) {
        return new StrategyPositionResponse(
                position.strategyId().value(),
                position.symbol().value(),
                position.quantity(),
                position.avgPrice().amount(),
                position.realizedPnl().amount());
    }

    private StrategyLogResponse toLogResponse(StrategyLog log) {
        return new StrategyLogResponse(
                log.strategyId().value(),
                log.level().name(),
                log.message(),
                log.payloadJson(),
                log.createdAt());
    }
}
