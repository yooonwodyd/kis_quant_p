package com.kisquant.execution.adapter.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.kisquant.execution.application.OpenOrderSyncUseCase;
import com.kisquant.operationlog.application.OperationLogBuffer;
import com.kisquant.operationlog.domain.OperationLogCategory;
import com.kisquant.operationlog.domain.OperationLogLevel;
import com.kisquant.shared.config.KisQuantProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class ExecutionStartupSynchronizerTest {

    @Test
    void synchronizesOpenOrdersOnStartupWhenStartupSyncIsEnabledEvenIfPollingIsDisabled() throws Exception {
        RecordingOpenOrderSyncUseCase syncUseCase = new RecordingOpenOrderSyncUseCase();
        OperationLogBuffer operationLogs = operationLogs();
        ExecutionStartupSynchronizer synchronizer = new ExecutionStartupSynchronizer(syncUseCase, properties(true, false, 7), operationLogs);

        synchronizer.run(new DefaultApplicationArguments());

        assertThat(syncUseCase.calledBatchSize).isEqualTo(7);
        assertThat(operationLogs.recent(null, 10))
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.level()).isEqualTo(OperationLogLevel.INFO);
                    assertThat(log.category()).isEqualTo(OperationLogCategory.STARTUP_SYNC);
                    assertThat(log.message()).contains("시작 체결 동기화 완료");
                });
    }

    @Test
    void doesNotSynchronizeOpenOrdersOnStartupWhenStartupSyncIsDisabled() throws Exception {
        RecordingOpenOrderSyncUseCase syncUseCase = new RecordingOpenOrderSyncUseCase();
        OperationLogBuffer operationLogs = operationLogs();
        ExecutionStartupSynchronizer synchronizer = new ExecutionStartupSynchronizer(syncUseCase, properties(false, true, 7), operationLogs);

        synchronizer.run(new DefaultApplicationArguments());

        assertThat(syncUseCase.calledBatchSize).isZero();
        assertThat(operationLogs.recent(null, 10)).isEmpty();
    }

    @Test
    void startupSyncFailureDoesNotAbortApplicationStartup() throws Exception {
        ThrowingOpenOrderSyncUseCase syncUseCase = new ThrowingOpenOrderSyncUseCase();
        OperationLogBuffer operationLogs = operationLogs();
        ExecutionStartupSynchronizer synchronizer = new ExecutionStartupSynchronizer(syncUseCase, properties(true, false, 7), operationLogs);

        synchronizer.run(new DefaultApplicationArguments());

        assertThat(syncUseCase.called).isTrue();
        assertThat(operationLogs.recent(null, 10))
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.level()).isEqualTo(OperationLogLevel.ERROR);
                    assertThat(log.category()).isEqualTo(OperationLogCategory.STARTUP_SYNC);
                    assertThat(log.message()).contains("시작 체결 동기화 실패", "KIS unavailable");
                });
    }

    private KisQuantProperties properties(boolean startupSyncEnabled, boolean pollingEnabled, int batchSize) {
        return new KisQuantProperties(
                new KisQuantProperties.Trading(List.of("001510"), 30_000L, 30_000L, 30_000L),
                new KisQuantProperties.StartupSync(startupSyncEnabled),
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
        private boolean called;

        @Override
        public int syncOpenOrders(int batchSize) {
            called = true;
            throw new IllegalStateException("KIS unavailable");
        }
    }
}
