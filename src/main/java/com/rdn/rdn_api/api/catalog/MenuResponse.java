package com.rdn.rdn_api.api.catalog;

import java.util.List;
import java.util.UUID;

import com.rdn.rdn_api.domain.Menu;
import com.rdn.rdn_api.domain.MenuItem;

/**
 * Cardapio de uma unidade na resposta HTTP.
 */
public record MenuResponse(UUID unidadeId, String unidadeNome, UUID cardapioId, String titulo,
        List<MenuItemResponse> itens) {

    public static MenuResponse from(Menu menu) {
        UUID unitId = menu.getBranch() == null ? null : menu.getBranch().getId();
        String unitName = menu.getBranch() == null ? null : menu.getBranch().getName();
        List<MenuItem> menuItems = menu.getItems() == null ? List.of() : menu.getItems();

        return new MenuResponse(unitId, unitName, menu.getId(), menu.getTitle(),
                menuItems.stream().map(MenuItemResponse::from).toList());
    }
}
