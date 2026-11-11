package com.rdn.rdn_api.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rdn.rdn_api.infrastructure.persistence.entity.InventoryItemJpaEntity;

/**
 * Acesso a tabela {@code inventory_items}.
 */
public interface InventoryItemJpaRepository extends JpaRepository<InventoryItemJpaEntity, UUID> {

    Optional<InventoryItemJpaEntity> findByBranchIdAndMenuItemId(UUID branchId, UUID menuItemId);

    List<InventoryItemJpaEntity> findByBranchId(UUID branchId);

}
