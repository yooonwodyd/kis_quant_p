package com.kisquant.execution.adapter.persistence;

import com.kisquant.execution.application.ExecutionRepository;
import com.kisquant.execution.domain.Execution;
import com.kisquant.execution.domain.ExecutionDedupKey;
import com.kisquant.shared.domain.OrderId;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaExecutionRepositoryAdapter implements ExecutionRepository {

    private final JpaExecutionRepository repository;

    public JpaExecutionRepositoryAdapter(JpaExecutionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Execution save(Execution execution) {
        return repository.saveAndFlush(JpaExecutionEntity.from(execution)).toDomain();
    }

    @Override
    public boolean existsByDedupKey(ExecutionDedupKey dedupKey) {
        return repository.existsByDedupKey(dedupKey.value());
    }

    @Override
    public List<Execution> findByOrderId(OrderId orderId) {
        return repository.findByOrderId(orderId.value()).stream().map(JpaExecutionEntity::toDomain).toList();
    }

    @Override
    public List<Execution> findAll() {
        return repository.findAll().stream().map(JpaExecutionEntity::toDomain).toList();
    }
}
