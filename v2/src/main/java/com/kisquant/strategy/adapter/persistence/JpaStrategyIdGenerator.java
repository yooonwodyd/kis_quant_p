package com.kisquant.strategy.adapter.persistence;

import com.kisquant.shared.domain.StrategyId;
import com.kisquant.strategy.application.StrategyIdGenerator;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JpaStrategyIdGenerator implements StrategyIdGenerator {

    private final AtomicLong sequence;

    public JpaStrategyIdGenerator(JdbcTemplate jdbcTemplate) {
        this.sequence = new AtomicLong(maxId(jdbcTemplate));
    }

    @Override
    public StrategyId nextStrategyId() {
        return StrategyId.of(sequence.incrementAndGet());
    }

    private static long maxId(JdbcTemplate jdbcTemplate) {
        Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM strategies", Long.class);
        return maxId == null ? 0L : maxId;
    }
}
