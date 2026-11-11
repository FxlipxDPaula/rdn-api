package com.rdn.rdn_api.infrastructure.persistence.mapper;

import com.rdn.rdn_api.domain.MenuItem;
import com.rdn.rdn_api.infrastructure.persistence.entity.MenuItemJpaEntity;

/**
 * Conversao entre {@link MenuItem} e {@link MenuItemJpaEntity}.
 *
 * <p>{@code MenuItem} nao conhece o cardapio a que pertence, logo nao ha
 * recursao nesta conversao.</p>
 */
public final class MenuItemMapper {

    private MenuItemMapper() {
    }

    public static MenuItem toDomain(MenuItemJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        MenuItem item = new MenuItem();
        item.setId(entity.getId());
        item.setName(entity.getName());
        item.setDescription(entity.getDescription());
        item.setPrice(entity.getPrice());
        item.setIsAvailable(entity.getIsAvailable());

        return item;
    }

    public static MenuItemJpaEntity toEntity(MenuItem domain) {
        if (domain == null) {
            return null;
        }

        MenuItemJpaEntity entity = new MenuItemJpaEntity();
        copyScalarsToEntity(domain, entity);

        return entity;
    }

    public static void copyScalarsToEntity(MenuItem domain, MenuItemJpaEntity entity) {
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setPrice(domain.getPrice());
        entity.setIsAvailable(domain.getIsAvailable());
    }

}
