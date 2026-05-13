package com.kisquant.order.application;

import com.kisquant.execution.domain.ExecutionId;
import com.kisquant.shared.domain.OrderId;
import java.util.concurrent.atomic.AtomicLong;

public class SequentialTradingIdGenerator implements TradingIdGenerator {

    private final AtomicLong orderSequence = new AtomicLong();
    private final AtomicLong executionSequence = new AtomicLong();

    @Override
    public OrderId nextOrderId() {
        return OrderId.of(orderSequence.incrementAndGet());
    }

    @Override
    public ExecutionId nextExecutionId() {
        return ExecutionId.of(executionSequence.incrementAndGet());
    }
}
