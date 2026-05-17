package com.kisquant.execution.adapter.scheduler;

import com.kisquant.execution.application.OpenOrderSyncUseCase;
import com.kisquant.operationlog.application.OperationLogBuffer;
import com.kisquant.operationlog.domain.OperationLogCategory;
import com.kisquant.operationlog.domain.OperationLogLevel;
import com.kisquant.shared.config.KisQuantProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExecutionPollingScheduler {

    private final OpenOrderSyncUseCase syncUseCase;
    private final KisQuantProperties properties;
    private final OperationLogBuffer operationLogs;

    public ExecutionPollingScheduler(
            OpenOrderSyncUseCase syncUseCase,
            KisQuantProperties properties,
            OperationLogBuffer operationLogs
    ) {
        this.syncUseCase = syncUseCase;
        this.properties = properties;
        this.operationLogs = operationLogs;
    }

    @Scheduled(fixedDelayString = "${kis-quant.polling.interval}")
    void poll() {
        if (!properties.polling().enabled()) {
            return;
        }
        try {
            int syncedExecutions = syncUseCase.syncOpenOrders(properties.polling().batchSize());
            operationLogs.append(
                    OperationLogLevel.INFO,
                    OperationLogCategory.POLLING,
                    "polling 완료: 신규 체결 " + syncedExecutions + "개",
                    "{\"syncedExecutions\":" + syncedExecutions + "}");
        } catch (RuntimeException exception) {
            operationLogs.append(
                    OperationLogLevel.ERROR,
                    OperationLogCategory.POLLING_ERROR,
                    "polling 실패: " + exception.getMessage(),
                    "{\"errorType\":\"" + jsonEscape(exception.getClass().getSimpleName()) + "\""
                            + ",\"message\":\"" + jsonEscape(exception.getMessage()) + "\"}");
        }
    }

    private String jsonEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
