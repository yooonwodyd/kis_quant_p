package com.kisquant.strategy.adapter.web;

import com.kisquant.order.domain.OrderSide;
import com.kisquant.order.domain.OrderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PlaceOrderRequest(
        @Min(1L) long strategyId,
        @NotBlank @Pattern(regexp = "\\d{6}") String symbol,
        @NotNull OrderSide side,
        @NotNull OrderType orderType,
        @Min(1L) long quantity,
        @Min(0L) long orderPrice
) {
}
