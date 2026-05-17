package com.kisquant.execution.adapter.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.kisquant.execution.application.OpenOrderSyncUseCase;
import com.kisquant.operationlog.application.OperationLogBuffer;
import com.kisquant.operationlog.domain.OperationLogCategory;
import com.kisquant.operationlog.domain.OperationLogLevel;
import com.kisquant.shared.config.KisQuantProperties;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExecutionPollingSchedulerTest {

    @Test
    void pollSynchronizesOpenOrdersWhenPollingIsEnabled() {
        RecordingOpenOrderSyncUseCase syncUseCase = new RecordingOpenOrderSyncUseCase();
        OperationLogBuffer operationLogs = operationLogs();
        ExecutionPollingScheduler scheduler = new ExecutionPollingScheduler(syncUseCase, properties(true, 11), operationLogs);

        scheduler.poll();

        assertThat(syncUseCase.calledBatchSize).isEqualTo(11);
        assertThat(operationLogs.recent(null, 10))
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.level()).isEqualTo(OperationLogLevel.INFO);
                    assertThat(log.category()).isEqualTo(OperationLogCategory.POLLING);
                    assertThat(log.message()).contains("polling 완료", "신규 체결 0개");
                });
    }

    @Test
    void pollDoesNotSynchronizeOpenOrdersWhenPollingIsDisabled() {
        RecordingOpenOrderSyncUseCase syncUseCase = new RecordingOpenOrderSyncUseCase();
        OperationLogBuffer operationLogs = operationLogs();
        ExecutionPollingScheduler scheduler = new ExecutionPollingScheduler(syncUseCase, properties(false, 11), operationLogs);

        scheduler.poll();

        assertThat(syncUseCase.calledBatchSize).isZero();
        assertThat(operationLogs.recent(null, 10)).isEmpty();
    }

    @Test
    void pollFailureIsShownAsOperationLog() {
        ThrowingOpenOrderSyncUseCase syncUseCase = new ThrowingOpenOrderSyncUseCase();
        OperationLogBuffer operationLogs = operationLogs();
        ExecutionPollingScheduler scheduler = new ExecutionPollingScheduler(syncUseCase, properties(true, 11), operationLogs);

        scheduler.poll();

        assertThat(operationLogs.recent(null, 10))
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.level()).isEqualTo(OperationLogLevel.ERROR);
                    assertThat(log.category()).isEqualTo(OperationLogCategory.POLLING_ERROR);
                    assertThat(log.message()).contains("polling 실패", "KIS unavailable");
                });
    }

    private KisQuantProperties properties(boolean pollingEnabled, int batchSize) {
        return new KisQuantProperties(
                new KisQuantProperties.Trading(List.of("001510"), 30_000L, 30_000L, 30_000L),
                new KisQuantProperties.StartupSync(true),
                new KisQuantProperties.Polling(pollingEnabled, Duration.ofSeconds(10L), batchSize),
                new KisQuantProperties.OperationLogs(100),
                new KisQuantProperties.Kis("https://openapi.koreainvestment.com:9443", "key", "secret", "12345678", "01", Duration.ofSeconds(3L)));
    }

    private OperationLogBuffer operationLogs() {
        return new OperationLogBuffer(() -> Instant.parse("2026-05-08T01:00:00Z"), 100);
    }

    private static final class RecordingOpenOrderSyncUseCase implements OpenOrderSyncUseCase {
        private int calledBatchSize;

        @Override
        public int syncOpenOrders(int batchSize) {
            this.calledBatchSize = batchSize;
            return 0;
        }
    }

    private static final class ThrowingOpenOrderSyncUseCase implements OpenOrderSyncUseCase {
        @Override
        public int syncOpenOrders(int batchSize) {
            throw new IllegalStateException("KIS unavailable");
        }
    }
}
