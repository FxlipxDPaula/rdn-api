package com.rdn.rdn_api.infrastructure.persistence.mapper;

import com.rdn.rdn_api.domain.InventoryItem;
import com.rdn.rdn_api.infrastructure.persistence.entity.InventoryItemJpaEntity;

/**
 * Conversao entre {@link InventoryItem} e {@link InventoryItemJpaEntity}.
 *
 * <p>O dominio guarda apenas o identificador do item de cardapio, enquanto a
 * entidade guarda a associacao: a conversao troca um pelo outro.</p>
 */
public final class InventoryItemMapper {

    private InventoryItemMapper() {
    }

    public static InventoryItem toDomain(InventoryItemJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        InventoryItem item = new InventoryItem();
        item.setId(entity.getId());
        item.setBranch(BranchMapper.toDomainShallow(entity.getBranch()));
        item.setMenuItemId(entity.getMenuItem() == null ? null : entity.getMenuItem().getId());
        item.setQuantityOnHand(entity.getQuantityOnHand());
        item.setMinimumThreshold(entity.getMinimumThreshold());

        return item;
    }

    public static InventoryItemJpaEntity toEntity(InventoryItem domain) {
        if (domain == null) {
            return null;
        }

        InventoryItemJpaEntity entity = new InventoryItemJpaEntity();
        copyScalarsToEntity(domain, entity);

        return entity;
    }

    public static void copyScalarsToEntity(InventoryItem domain, InventoryItemJpaEntity entity) {
        entity.setId(domain.getId());
        entity.setQuantityOnHand(domain.getQuantityOnHand());
        entity.setMinimumThreshold(domain.getMinimumThreshold());
    }

}
