package com.rdn.rdn_api.infrastructure.persistence.mapper;

import java.util.ArrayList;
import java.util.List;

import com.rdn.rdn_api.domain.Menu;
import com.rdn.rdn_api.domain.MenuItem;
import com.rdn.rdn_api.infrastructure.persistence.entity.MenuItemJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.MenuJpaEntity;

/**
 * Conversao entre {@link Menu} e {@link MenuJpaEntity}.
 *
 * <p>A filial e montada em modo reduzido ({@link BranchMapper#toDomainShallow})
 * para evitar o ciclo {@code Branch -> Menu -> Branch}.</p>
 */
public final class MenuMapper {

    private MenuMapper() {
    }

    public static Menu toDomain(MenuJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Menu menu = new Menu();
        menu.setId(entity.getId());
        menu.setTitle(entity.getTitle());
        menu.setIsActive(entity.getIsActive());
        menu.setBranch(BranchMapper.toDomainShallow(entity.getBranch()));

        List<MenuItem> items = new ArrayList<>();
        for (MenuItemJpaEntity item : entity.getItems()) {
            items.add(MenuItemMapper.toDomain(item));
        }
        menu.setItems(items);

        return menu;
    }

    public static MenuJpaEntity toEntity(Menu domain) {
        if (domain == null) {
            return null;
        }

        MenuJpaEntity entity = new MenuJpaEntity();
        copyScalarsToEntity(domain, entity);

        return entity;
    }

    public static void copyScalarsToEntity(Menu domain, MenuJpaEntity entity) {
        entity.setId(domain.getId());
        entity.setTitle(domain.getTitle());
        entity.setIsActive(domain.getIsActive());
    }

}
