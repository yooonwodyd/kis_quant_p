package com.kisquant.strategy.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface JpaStrategyRepository extends JpaRepository<JpaStrategyEntity, Long> {
}
