package com.kisquant.execution.adapter.scheduler;

import com.kisquant.execution.application.OpenOrderSyncUseCase;
import com.kisquant.operationlog.application.OperationLogBuffer;
import com.kisquant.operationlog.domain.OperationLogCategory;
import com.kisquant.operationlog.domain.OperationLogLevel;
import com.kisquant.shared.config.KisQuantProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ExecutionStartupSynchronizer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExecutionStartupSynchronizer.class);

    private final OpenOrderSyncUseCase syncUseCase;
    private final KisQuantProperties properties;
    private final OperationLogBuffer operationLogs;

    public ExecutionStartupSynchronizer(
            OpenOrderSyncUseCase syncUseCase,
            KisQuantProperties properties,
            OperationLogBuffer operationLogs
    ) {
        this.syncUseCase = syncUseCase;
        this.properties = properties;
        this.operationLogs = operationLogs;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.startupSync().enabled()) {
            return;
        }
        try {
            int syncedExecutions = syncUseCase.syncOpenOrders(properties.polling().batchSize());
            operationLogs.append(
                    OperationLogLevel.INFO,
                    OperationLogCategory.STARTUP_SYNC,
                    "시작 체결 동기화 완료: 신규 체결 " + syncedExecutions + "개",
                    "{\"syncedExecutions\":" + syncedExecutions + "}");
        } catch (RuntimeException exception) {
            log.warn("Startup execution synchronization failed. Application startup will continue.", exception);
            operationLogs.append(
                    OperationLogLevel.ERROR,
                    OperationLogCategory.STARTUP_SYNC,
                    "시작 체결 동기화 실패: " + exception.getMessage(),
                    "{\"errorType\":\"" + jsonEscape(exception.getClass().getSimpleName()) + "\""
                            + ",\"message\":\"" + jsonEscape(exception.getMessage()) + "\"}");
        }
    }

    private String jsonEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
