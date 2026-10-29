package com.rdn.rdn_api.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cardapio de uma filial.
 *
 * <p>POJO de dominio: apenas campos, construtor padrao, getters/setters e
 * operacoes triviais sobre a lista de itens. Nao contem anotacoes de persistencia
 * nem regras de negocio (sem validacoes e sem excecoes de dominio).</p>
 */
public class Menu {

    private UUID id;

    private Branch branch;

    private String title;

    private Boolean isActive;

    private List<MenuItem> items = new ArrayList<>();

    public Menu() {
    }

    /**
     * Adiciona um item ao cardapio.
     *
     * @param item item a ser adicionado
     */
    public void addItem(MenuItem item) {
        items.add(item);
    }

    /**
     * Remove do cardapio o item cujo identificador corresponde ao informado.
     * Nao lanca excecao quando o item nao existe.
     *
     * @param itemId identificador do item do cardapio
     */
    public void removeItem(UUID itemId) {
        items.removeIf(item -> item.getId() != null && item.getId().equals(itemId));
    }

    /**
     * Retorna apenas os itens marcados como disponiveis.
     *
     * @return nova lista contendo os itens com {@code isAvailable} verdadeiro
     */
    public List<MenuItem> getAvailableItems() {
        List<MenuItem> available = new ArrayList<>();
        for (MenuItem item : items) {
            if (Boolean.TRUE.equals(item.getIsAvailable())) {
                available.add(item);
            }
        }

        return available;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public List<MenuItem> getItems() {
        return items;
    }

    public void setItems(List<MenuItem> items) {
        this.items = items;
    }

}
