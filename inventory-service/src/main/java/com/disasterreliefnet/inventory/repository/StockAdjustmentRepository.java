package com.disasterreliefnet.inventory.repository;

import com.disasterreliefnet.inventory.domain.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, UUID> {

    List<StockAdjustment> findByItemIdOrderByCreatedAtDesc(UUID itemId);
}
