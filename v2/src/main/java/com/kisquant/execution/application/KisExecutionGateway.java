package com.kisquant.execution.application;

import com.kisquant.order.domain.Order;
import java.util.List;

@FunctionalInterface
public interface KisExecutionGateway {

    List<KisExecutionSnapshot> findExecutionSnapshots(Order order);
}
