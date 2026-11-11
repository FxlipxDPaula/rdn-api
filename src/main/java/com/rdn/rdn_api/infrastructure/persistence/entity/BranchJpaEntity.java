package com.rdn.rdn_api.infrastructure.persistence.entity;

import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Mapeamento relacional de {@code Branch}.
 *
 * <p>O cardapio da filial e o lado inverso do relacionamento: a chave
 * estrangeira {@code branch_id} fica em {@code menus}, com restricao de
 * unicidade, pois cada filial possui no maximo um cardapio.</p>
 */
@Entity
@Table(name = "branches")
public class BranchJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "is_open", nullable = false)
    private Boolean isOpen;

    @Embedded
    private AddressEmbeddable address;

    @OneToOne(mappedBy = "branch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private MenuJpaEntity menu;

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

    public Boolean getIsOpen() {
        return isOpen;
    }

    public void setIsOpen(Boolean isOpen) {
        this.isOpen = isOpen;
    }

    public AddressEmbeddable getAddress() {
        return address;
    }

    public void setAddress(AddressEmbeddable address) {
        this.address = address;
    }

    public MenuJpaEntity getMenu() {
        return menu;
    }

    public void setMenu(MenuJpaEntity menu) {
        this.menu = menu;
    }

}
