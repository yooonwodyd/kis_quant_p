package com.kisquant.strategy.adapter.web;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.strategy.application.AdminStrategyApplicationService;
import com.kisquant.strategy.application.CreateStrategyCommand;
import com.kisquant.strategy.domain.Strategy;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final AdminStrategyApplicationService strategyService;

    public AdminApiController(AdminStrategyApplicationService strategyService) {
        this.strategyService = strategyService;
    }

    @GetMapping("/strategies")
    public List<AdminStrategyResponse> strategies() {
        return strategyService.findStrategies().stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/strategies")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminStrategyResponse createStrategy(@Valid @RequestBody CreateAdminStrategyRequest request) {
        Strategy strategy = strategyService.createStrategy(new CreateStrategyCommand(
                request.name(),
                request.tradeMode(),
                Money.won(request.initialBudgetAmount()),
                Money.won(request.maxOrderAmount()),
                Money.won(request.maxDailyOrderAmount())));
        return toResponse(strategy);
    }

    @PatchMapping("/strategies/{strategyId}/status")
    public AdminStrategyResponse changeStatus(
            @PathVariable long strategyId,
            @Valid @RequestBody ChangeStrategyStatusRequest request
    ) {
        return toResponse(strategyService.changeEnabled(StrategyId.of(strategyId), request.enabled()));
    }

    @PatchMapping("/strategies/{strategyId}/trade-mode")
    public AdminStrategyResponse changeTradeMode(
            @PathVariable long strategyId,
            @Valid @RequestBody ChangeStrategyTradeModeRequest request
    ) {
        return toResponse(strategyService.changeTradeMode(StrategyId.of(strategyId), request.tradeMode()));
    }

    @PatchMapping("/strategies/{strategyId}/budget")
    public AdminStrategyResponse changeBudget(
            @PathVariable long strategyId,
            @Valid @RequestBody ChangeStrategyBudgetRequest request
    ) {
        return toResponse(strategyService.changeInitialBudget(
                StrategyId.of(strategyId),
                Money.won(request.initialBudgetAmount())));
    }

    @DeleteMapping("/strategies/{strategyId}/positions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPositions(@PathVariable long strategyId) {
        strategyService.resetPositions(StrategyId.of(strategyId));
    }

    private AdminStrategyResponse toResponse(Strategy strategy) {
        return new AdminStrategyResponse(
                strategy.id().value(),
                strategy.name(),
                strategy.enabled(),
                strategy.tradeMode().name(),
                strategy.initialBudget().amount(),
                strategy.maxOrderAmount().amount(),
                strategy.maxDailyOrderAmount().amount());
    }
}
