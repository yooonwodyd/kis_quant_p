package com.kisquant.order.adapter.web;

import com.kisquant.account.application.AccountQueryService;
import com.kisquant.account.application.BuyableOrderAmount;
import com.kisquant.account.application.Holding;
import com.kisquant.marketdata.application.DailyCandle;
import com.kisquant.marketdata.application.MarketDataQueryService;
import com.kisquant.order.application.CancelOrderApplicationService;
import com.kisquant.order.application.OrderQueryService;
import com.kisquant.order.application.PlaceOrderApplicationService;
import com.kisquant.order.application.PlaceOrderCommand;
import com.kisquant.order.application.PlaceOrderResult;
import com.kisquant.order.domain.Order;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.strategy.adapter.web.OrderResponse;
import com.kisquant.strategy.adapter.web.PlaceOrderRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/kis")
public class OperationApiController {

    private final AccountQueryService accountQueryService;
    private final MarketDataQueryService marketDataQueryService;
    private final PlaceOrderApplicationService placeOrderService;
    private final OrderQueryService orderQueryService;
    private final CancelOrderApplicationService cancelOrderService;
    private final List<Symbol> allowedSymbols;

    public OperationApiController(
            AccountQueryService accountQueryService,
            MarketDataQueryService marketDataQueryService,
            PlaceOrderApplicationService placeOrderService,
            OrderQueryService orderQueryService,
            CancelOrderApplicationService cancelOrderService,
            List<Symbol> allowedSymbols
    ) {
        this.accountQueryService = accountQueryService;
        this.marketDataQueryService = marketDataQueryService;
        this.placeOrderService = placeOrderService;
        this.orderQueryService = orderQueryService;
        this.cancelOrderService = cancelOrderService;
        this.allowedSymbols = List.copyOf(allowedSymbols);
    }

    @GetMapping("/symbols")
    public List<SymbolResponse> symbols() {
        return allowedSymbols.stream()
                .map(symbol -> new SymbolResponse(symbol.value()))
                .toList();
    }

    @GetMapping("/account/balance")
    public BalanceResponse balance() {
        return new BalanceResponse(accountQueryService.balance().cash().amount());
    }

    @GetMapping("/account/holdings")
    public List<HoldingResponse> holdings() {
        return accountQueryService.holdings().stream().map(this::toHoldingResponse).toList();
    }

    @GetMapping("/quotes/{symbol}")
    public QuoteResponse quote(@PathVariable @Pattern(regexp = "\\d{6}") String symbol) {
        var quote = marketDataQueryService.quote(Symbol.of(symbol));
        return new QuoteResponse(quote.symbol().value(), quote.currentPrice().amount());
    }

    @GetMapping("/daily-candles/{symbol}")
    public List<DailyCandleResponse> dailyCandles(
            @PathVariable @Pattern(regexp = "\\d{6}") String symbol,
            @RequestParam(defaultValue = "260") @Min(1L) int limit
    ) {
        return marketDataQueryService.dailyCandles(Symbol.of(symbol), limit).stream()
                .map(this::toDailyCandleResponse)
                .toList();
    }

    @GetMapping("/orders/buyable")
    public BuyableResponse buyable(
            @RequestParam @Pattern(regexp = "\\d{6}") String symbol,
            @RequestParam @Min(0L) long price
    ) {
        BuyableOrderAmount buyable = accountQueryService.buyable(Symbol.of(symbol), Money.won(price));
        return new BuyableResponse(buyable.symbol().value(), buyable.orderableCash().amount(), buyable.maxBuyQuantity());
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
        return new OrderResponse(result.orderId().value(), result.status().name(), result.kisOrderNumber().orElse(null), result.message());
    }

    @GetMapping("/orders/{orderId}")
    public OrderResponse order(@PathVariable @Min(1L) long orderId) {
        Order order = orderQueryService.findOrder(OrderId.of(orderId))
                .orElseThrow(() -> new IllegalArgumentException("order not found"));
        return toOrderResponse(order, null);
    }

    @PostMapping("/orders/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable @Min(1L) long orderId) {
        return toOrderResponse(cancelOrderService.cancel(OrderId.of(orderId)), "canceled");
    }

    private HoldingResponse toHoldingResponse(Holding holding) {
        return new HoldingResponse(
                holding.symbol().value(),
                holding.name(),
                holding.holdingQuantity(),
                holding.orderableQuantity(),
                holding.averagePrice().amount(),
                holding.currentPrice().amount(),
                holding.valuationAmount().amount());
    }

    private DailyCandleResponse toDailyCandleResponse(DailyCandle candle) {
        return new DailyCandleResponse(candle.date(), candle.closePrice().amount());
    }

    private OrderResponse toOrderResponse(Order order, String message) {
        return new OrderResponse(
                order.id().value(),
                order.status().name(),
                order.kisOrderNumber().map(kisOrderNumber -> kisOrderNumber.value()).orElse(null),
                message);
    }

    public record BalanceResponse(long cash) {
    }

    public record HoldingResponse(
            String symbol,
            String name,
            long holdingQuantity,
            long orderableQuantity,
            long averagePrice,
            long currentPrice,
            long valuationAmount
    ) {
    }

    public record QuoteResponse(String symbol, long currentPrice) {
    }

    public record DailyCandleResponse(LocalDate date, long closePrice) {
    }

    public record BuyableResponse(String symbol, long orderableCash, long maxBuyQuantity) {
    }

    public record SymbolResponse(String symbol) {
    }
}
