package com.rdn.rdn_api.application.order;

import java.util.Optional;
import java.util.UUID;

import com.rdn.rdn_api.domain.Branch;
import com.rdn.rdn_api.domain.MenuItem;

/**
 * Porta de saida para as referencias de catalogo usadas na criacao do pedido.
 */
public interface CatalogGateway {

    /**
     * Busca a unidade (filial) pelo identificador.
     *
     * @param branchId identificador da unidade
     * @return unidade, ou vazio quando nao existe
     */
    Optional<Branch> findBranch(UUID branchId);

    /**
     * Busca o item de cardapio pelo identificador.
     *
     * @param menuItemId identificador do item
     * @return item com preco e disponibilidade atuais, ou vazio
     */
    Optional<MenuItem> findMenuItem(UUID menuItemId);
}
