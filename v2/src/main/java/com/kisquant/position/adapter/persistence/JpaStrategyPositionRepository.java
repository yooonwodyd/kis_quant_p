package com.kisquant.position.adapter.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JpaStrategyPositionRepository extends JpaRepository<JpaStrategyPositionEntity, JpaStrategyPositionId> {

    List<JpaStrategyPositionEntity> findByIdStrategyId(Long strategyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from JpaStrategyPositionEntity position where position.id.strategyId = :strategyId")
    void deleteByStrategyId(@Param("strategyId") Long strategyId);
}
