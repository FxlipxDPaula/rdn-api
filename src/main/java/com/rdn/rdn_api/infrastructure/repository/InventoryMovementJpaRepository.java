package com.rdn.rdn_api.infrastructure.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rdn.rdn_api.infrastructure.persistence.entity.InventoryMovementJpaEntity;

/**
 * Acesso a tabela {@code inventory_movements}.
 */
public interface InventoryMovementJpaRepository
        extends JpaRepository<InventoryMovementJpaEntity, UUID> {

    List<InventoryMovementJpaEntity> findByInventoryItemIdOrderByTimestampDesc(UUID inventoryItemId);

}
