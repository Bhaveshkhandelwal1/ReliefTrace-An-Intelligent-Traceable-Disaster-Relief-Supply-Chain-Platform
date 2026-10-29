package com.disasterreliefnet.inventory.repository;

import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.inventory.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID>, JpaSpecificationExecutor<Item> {

    Optional<Item> findByItemIdAndDeletedAtIsNull(UUID itemId);

    Page<Item> findByDeletedAtIsNull(Pageable pageable);

    Page<Item> findByCategoryAndDeletedAtIsNull(AidCategory category, Pageable pageable);

    Page<Item> findByWarehouseIdAndDeletedAtIsNull(UUID warehouseId, Pageable pageable);

    Page<Item> findByCategoryAndWarehouseIdAndDeletedAtIsNull(AidCategory category, UUID warehouseId, Pageable pageable);

    List<Item> findByWarehouseIdAndDeletedAtIsNull(UUID warehouseId);

    @Query("SELECT i FROM Item i WHERE i.deletedAt IS NULL AND (i.quantityOnHand - i.quantityReserved) <= :threshold")
    List<Item> findLowStockItems(@Param("threshold") BigDecimal threshold);

    List<Item> findByExpiresAtBeforeAndDeletedAtIsNull(Instant cutoff);

    boolean existsByWarehouseIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID warehouseId, String name);
}
