package com.kisquant.order.adapter.persistence;

import com.kisquant.execution.domain.ExecutionId;
import com.kisquant.order.application.TradingIdGenerator;
import com.kisquant.shared.domain.OrderId;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JpaTradingIdGenerator implements TradingIdGenerator {

    private final AtomicLong orderSequence;
    private final AtomicLong executionSequence;

    public JpaTradingIdGenerator(JdbcTemplate jdbcTemplate) {
        this.orderSequence = new AtomicLong(maxId(jdbcTemplate, "orders"));
        this.executionSequence = new AtomicLong(maxId(jdbcTemplate, "executions"));
    }

    @Override
    public OrderId nextOrderId() {
        return OrderId.of(orderSequence.incrementAndGet());
    }

    @Override
    public ExecutionId nextExecutionId() {
        return ExecutionId.of(executionSequence.incrementAndGet());
    }

    private static long maxId(JdbcTemplate jdbcTemplate, String tableName) {
        Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM " + tableName, Long.class);
        return maxId == null ? 0L : maxId;
    }
}
