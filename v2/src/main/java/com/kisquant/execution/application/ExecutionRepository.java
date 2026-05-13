package com.kisquant.execution.application;

import com.kisquant.execution.domain.Execution;
import com.kisquant.execution.domain.ExecutionDedupKey;
import com.kisquant.shared.domain.OrderId;
import java.util.List;

public interface ExecutionRepository {

    Execution save(Execution execution);

    boolean existsByDedupKey(ExecutionDedupKey dedupKey);

    List<Execution> findByOrderId(OrderId orderId);

    List<Execution> findAll();
}
