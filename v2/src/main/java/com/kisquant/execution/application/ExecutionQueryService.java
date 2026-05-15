package com.kisquant.execution.application;

import com.kisquant.execution.domain.Execution;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutionQueryService {

    private final ExecutionRepository repository;

    public ExecutionQueryService(ExecutionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Execution> findExecutions() {
        return repository.findAll();
    }
}
