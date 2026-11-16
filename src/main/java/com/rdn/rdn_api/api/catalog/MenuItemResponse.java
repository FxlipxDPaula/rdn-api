package com.rdn.rdn_api.api.catalog;

import java.math.BigDecimal;
import java.util.UUID;

import com.rdn.rdn_api.domain.MenuItem;

/**
 * Item de cardapio na resposta HTTP.
 */
public record MenuItemResponse(UUID produtoId, String nome, String descricao, BigDecimal preco,
        boolean disponivel) {

    public static MenuItemResponse from(MenuItem item) {
        return new MenuItemResponse(item.getId(), item.getName(), item.getDescription(),
                item.getPrice(), item.isAvailable());
    }
}
