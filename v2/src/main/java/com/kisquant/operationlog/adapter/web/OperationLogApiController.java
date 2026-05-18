package com.kisquant.operationlog.adapter.web;

import com.kisquant.operationlog.application.OperationLogBuffer;
import com.kisquant.operationlog.domain.OperationLogCategory;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin")
public class OperationLogApiController {

    private final OperationLogBuffer operationLogs;

    public OperationLogApiController(OperationLogBuffer operationLogs) {
        this.operationLogs = operationLogs;
    }

    @GetMapping("/operation-logs")
    public List<OperationLogResponse> operationLogs(
            @RequestParam(required = false) OperationLogCategory category,
            @RequestParam(defaultValue = "100") @Min(1L) int limit
    ) {
        return operationLogs.recent(category, limit).stream()
                .map(log -> new OperationLogResponse(
                        log.id(),
                        log.level().name(),
                        log.category().name(),
                        log.message(),
                        log.payloadJson(),
                        log.createdAt()))
                .toList();
    }

    public record OperationLogResponse(
            long id,
            String level,
            String category,
            String message,
            String payloadJson,
            Instant createdAt
    ) {
    }
}
