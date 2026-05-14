package com.kisquant.strategy.application;

import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.shared.domain.TradeMode;
import com.kisquant.shared.time.CurrentTimeProvider;
import com.kisquant.order.domain.OrderStatus;
import com.kisquant.position.application.PositionResetReason;
import com.kisquant.strategylog.application.StrategyLogRepository;
import com.kisquant.strategylog.domain.LogLevel;
import com.kisquant.strategylog.domain.StrategyLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminStrategyLogService {

    private final StrategyLogRepository logRepository;
    private final CurrentTimeProvider timeProvider;

    public AdminStrategyLogService(StrategyLogRepository logRepository, CurrentTimeProvider timeProvider) {
        this.logRepository = logRepository;
        this.timeProvider = timeProvider;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordManualPositionResetSuccess(StrategyId strategyId) {
        save(strategyId, LogLevel.INFO, "관리자 포지션 초기화 완료", "{\"event\":\"POSITION_RESET\",\"reason\":\"MANUAL\",\"status\":\"SUCCESS\"}");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPositionResetSuccess(StrategyId strategyId, PositionResetReason reason) {
        if (reason == PositionResetReason.TRADE_MODE_CHANGE) {
            save(strategyId, LogLevel.INFO, "투자 모드 변경으로 모의 포지션 초기화 완료",
                    "{\"event\":\"POSITION_RESET\",\"reason\":\"TRADE_MODE_CHANGE\",\"status\":\"SUCCESS\"}");
            return;
        }
        recordManualPositionResetSuccess(strategyId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTradeModePositionResetSuccess(StrategyId strategyId, TradeMode previousMode, TradeMode nextMode) {
        save(
                strategyId,
                LogLevel.INFO,
                "투자 모드 변경으로 포지션 초기화 완료: " + previousMode.name() + " -> " + nextMode.name(),
                "{\"event\":\"POSITION_RESET\",\"reason\":\"TRADE_MODE_CHANGE\",\"status\":\"SUCCESS\","
                        + "\"previousTradeMode\":\"" + previousMode.name() + "\","
                        + "\"nextTradeMode\":\"" + nextMode.name() + "\"}");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPositionResetFailure(StrategyId strategyId, String reason, RuntimeException exception) {
        save(
                strategyId,
                LogLevel.ERROR,
                "포지션 초기화 실패: " + exception.getMessage(),
                "{\"event\":\"POSITION_RESET\",\"reason\":\"" + reason + "\",\"status\":\"FAILED\"}");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLivePositionResetSkipped(StrategyId strategyId, PositionResetReason reason, String message) {
        save(
                strategyId,
                LogLevel.WARN,
                "LIVE 포지션 정리 건너뜀: " + message,
                "{\"event\":\"POSITION_RESET\",\"reason\":\"" + reason.name() + "\",\"status\":\"SKIPPED\"}");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLivePositionResetOrderResult(
            StrategyId strategyId,
            PositionResetReason reason,
            Symbol symbol,
            long quantity,
            OrderStatus status,
            String message
    ) {
        String suffix = message == null || message.isBlank() ? "" : " " + message;
        save(
                strategyId,
                status == OrderStatus.REJECTED ? LogLevel.ERROR : LogLevel.INFO,
                "LIVE 포지션 정리 매도 결과: " + symbol.value() + " " + quantity + "주 " + status.name() + suffix,
                "{\"event\":\"POSITION_RESET\",\"reason\":\"" + reason.name() + "\",\"status\":\""
                        + status.name() + "\",\"symbol\":\"" + symbol.value() + "\",\"quantity\":" + quantity + "}");
    }

    private void save(StrategyId strategyId, LogLevel level, String message, String payloadJson) {
        logRepository.save(StrategyLog.create(strategyId, level, message, payloadJson, timeProvider.now()));
    }
}
