package com.kisquant.execution.adapter.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaExecutionRepository extends JpaRepository<JpaExecutionEntity, Long> {

    boolean existsByDedupKey(String dedupKey);

    List<JpaExecutionEntity> findByOrderId(Long orderId);
}
