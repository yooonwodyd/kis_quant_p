package com.kisquant.shared.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("kis-quant")
public record KisQuantProperties(
        @Valid @NotNull Trading trading,
        @Valid @NotNull StartupSync startupSync,
        @Valid @NotNull Polling polling,
        @Valid @NotNull OperationLogs operationLogs,
        @Valid @NotNull Kis kis
) {

    public record Trading(
            @NotEmpty List<String> allowedSymbols,
            @Min(1L) long strategyInitialBudgetKrw,
            @Min(1L) long maxOrderKrw,
            @Min(1L) long maxDailyOrderKrw
    ) {
        public Trading {
            allowedSymbols = List.copyOf(allowedSymbols);
        }
    }

    public record Kis(
            String baseUrl,
            String appKey,
            String appSecret,
            String accountNumber,
            String accountProductCode,
            @NotNull Duration timeout
    ) {
    }

    public record Polling(
            boolean enabled,
            @NotNull Duration interval,
            @Min(1L) int batchSize
    ) {
    }

    public record StartupSync(
            boolean enabled
    ) {
    }

    public record OperationLogs(
            @Min(1L) int capacity
    ) {
    }
}
