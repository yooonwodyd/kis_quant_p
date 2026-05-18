package com.kisquant.strategy.adapter.web;

import jakarta.validation.constraints.Min;

public record ChangeStrategyBudgetRequest(@Min(1L) long initialBudgetAmount) {
}
