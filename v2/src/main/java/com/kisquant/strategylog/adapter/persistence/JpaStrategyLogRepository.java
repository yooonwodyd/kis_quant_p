package com.kisquant.strategylog.adapter.persistence;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaStrategyLogRepository extends JpaRepository<JpaStrategyLogEntity, Long> {

    List<JpaStrategyLogEntity> findByStrategyIdOrderByCreatedAtDesc(Long strategyId, Pageable pageable);

    List<JpaStrategyLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
