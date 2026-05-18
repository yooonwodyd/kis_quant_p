package com.kisquant.strategy.adapter.web;

import jakarta.validation.constraints.NotNull;

public record ChangeStrategyStatusRequest(@NotNull Boolean enabled) {
}
