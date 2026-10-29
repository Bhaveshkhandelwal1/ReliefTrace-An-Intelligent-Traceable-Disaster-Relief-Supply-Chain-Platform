package com.disasterreliefnet.inventory.repository;

import com.disasterreliefnet.inventory.domain.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

    Optional<Warehouse> findByWarehouseIdAndDeletedAtIsNull(UUID warehouseId);

    Page<Warehouse> findByDeletedAtIsNull(Pageable pageable);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);
}
