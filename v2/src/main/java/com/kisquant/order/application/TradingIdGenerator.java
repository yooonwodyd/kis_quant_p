package com.kisquant.order.application;

import com.kisquant.execution.domain.ExecutionId;
import com.kisquant.shared.domain.OrderId;

public interface TradingIdGenerator {

    OrderId nextOrderId();

    ExecutionId nextExecutionId();
}
