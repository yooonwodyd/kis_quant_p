package com.kisquant.position.domain;

import com.kisquant.execution.domain.Execution;
import java.util.Objects;

public final class PositionUpdateService {

    public void apply(StrategyPosition position, Execution execution) {
        Objects.requireNonNull(position, "position must not be null")
                .applyExecution(Objects.requireNonNull(execution, "execution must not be null"));
    }
}
