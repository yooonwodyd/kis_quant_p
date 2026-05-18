package com.kisquant.operationlog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.kisquant.operationlog.domain.OperationLogCategory;
import com.kisquant.operationlog.domain.OperationLogLevel;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OperationLogBufferTest {

    @Test
    void keepsRecentLogsInMemoryAndFiltersByCategory() {
        OperationLogBuffer buffer = new OperationLogBuffer(() -> Instant.parse("2026-05-08T01:00:00Z"), 2);

        buffer.append(OperationLogLevel.INFO, OperationLogCategory.STARTUP_SYNC, "시작 체결 동기화 완료", "{}");
        buffer.append(OperationLogLevel.INFO, OperationLogCategory.POLLING, "polling 완료", "{}");
        buffer.append(OperationLogLevel.ERROR, OperationLogCategory.POLLING_ERROR, "polling 실패", "{}");

        assertThat(buffer.recent(null, 10))
                .extracting(log -> log.message())
                .containsExactly("polling 실패", "polling 완료");
        assertThat(buffer.recent(OperationLogCategory.POLLING, 10))
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.level()).isEqualTo(OperationLogLevel.INFO);
                    assertThat(log.message()).isEqualTo("polling 완료");
                });
    }
}
