package com.rdn.rdn_api.infrastructure.persistence.mapper;

import com.rdn.rdn_api.domain.InventoryMovement;
import com.rdn.rdn_api.infrastructure.persistence.entity.InventoryMovementJpaEntity;

/**
 * Conversao entre {@link InventoryMovement} e
 * {@link InventoryMovementJpaEntity}.
 */
public final class InventoryMovementMapper {

    private InventoryMovementMapper() {
    }

    public static InventoryMovement toDomain(InventoryMovementJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        InventoryMovement movement = new InventoryMovement();
        movement.setId(entity.getId());
        movement.setInventoryItem(InventoryItemMapper.toDomain(entity.getInventoryItem()));
        movement.setType(entity.getType());
        movement.setQuantity(entity.getQuantity());
        movement.setTimestamp(entity.getTimestamp());
        movement.setNotes(entity.getNotes());

        return movement;
    }

    public static InventoryMovementJpaEntity toEntity(InventoryMovement domain) {
        if (domain == null) {
            return null;
        }

        InventoryMovementJpaEntity entity = new InventoryMovementJpaEntity();
        copyScalarsToEntity(domain, entity);

        return entity;
    }

    public static void copyScalarsToEntity(InventoryMovement domain,
            InventoryMovementJpaEntity entity) {
        entity.setId(domain.getId());
        entity.setType(domain.getType());
        entity.setQuantity(domain.getQuantity());
        entity.setTimestamp(domain.getTimestamp());
        entity.setNotes(domain.getNotes());
    }

}
