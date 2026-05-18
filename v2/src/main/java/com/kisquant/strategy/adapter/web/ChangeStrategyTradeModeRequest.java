package com.kisquant.strategy.adapter.web;

import com.kisquant.shared.domain.TradeMode;
import jakarta.validation.constraints.NotNull;

public record ChangeStrategyTradeModeRequest(@NotNull TradeMode tradeMode) {
}
