package com.rdn.rdn_api.domain;

import java.util.UUID;

/**
 * Filial da rede.
 *
 * <p>POJO de dominio: apenas campos, construtor padrao, getters/setters e
 * operacoes triviais. Nao contem anotacoes de persistencia nem regras de
 * negocio (sem validacoes e sem excecoes de dominio).</p>
 *
 * <p>O campo {@code menu} nao estava na especificacao original de atributos,
 * mas foi adicionado porque {@code getMenu()} e
 * {@code checkAvailability(UUID)} precisam de onde obter o cardapio.</p>
 */
public class Branch {

    private UUID id;

    private String name;

    private Address address;

    private Boolean isOpen;

    private Menu menu;

    public Branch() {
    }

    /**
     * Retorna o cardapio associado a filial.
     *
     * @return cardapio da filial, ou {@code null} quando nao carregado
     */
    public Menu getMenu() {
        return menu;
    }

    /**
     * Define o cardapio associado a filial.
     *
     * @param menu cardapio da filial
     */
    public void setMenu(Menu menu) {
        this.menu = menu;
    }

    /**
     * Informa se a filial esta apta a atender o item informado.
     *
     * <p>Verificacao: exige que a filial esteja aberta, que o cardapio esteja
     * carregado e que o item exista nele marcado como disponivel.</p>
     *
     * @param menuItemId identificador do item do cardapio
     * @return {@code true} quando a filial esta aberta e o item esta disponivel
     *         no cardapio
     */
    public Boolean checkAvailability(UUID menuItemId) {
        if (!Boolean.TRUE.equals(isOpen) || menu == null || menu.getItems() == null) {
            return Boolean.FALSE;
        }

        for (MenuItem item : menu.getItems()) {
            if (item.getId() != null
                    && item.getId().equals(menuItemId)
                    && Boolean.TRUE.equals(item.getIsAvailable())) {
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Boolean getIsOpen() {
        return isOpen;
    }

    public void setIsOpen(Boolean isOpen) {
        this.isOpen = isOpen;
    }

    public boolean isOpen() {
        return Boolean.TRUE.equals(isOpen);
    }

}
