package com.rdn.rdn_api.application.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.rdn.rdn_api.domain.Branch;
import com.rdn.rdn_api.domain.Menu;
import com.rdn.rdn_api.domain.MenuItem;

/**
 * Porta de saida para as referencias de catalogo usadas na criacao do pedido
 * e na consulta de unidades/cardapio.
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

    /**
     * Lista as unidades abertas.
     *
     * @return unidades com endereco, sem cardapio carregado
     */
    List<Branch> findOpenBranches();

    /**
     * Busca o cardapio da unidade com os itens.
     *
     * @param branchId identificador da unidade
     * @return cardapio com itens, ou vazio quando a unidade nao tem cardapio
     */
    Optional<Menu> findMenuByBranchId(UUID branchId);
}
