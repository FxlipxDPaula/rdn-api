package com.rdn.rdn_api.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Mapeamento relacional de {@code InventoryItem}.
 *
 * <p>O estoque e sempre por filial: a unicidade e garantida pelo par
 * {@code branch_id} + {@code menu_item_id}.</p>
 */
@Entity
@Table(name = "inventory_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_branch_item",
                columnNames = {"branch_id", "menu_item_id"}))
public class InventoryItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchJpaEntity branch;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItemJpaEntity menuItem;

    @Column(name = "quantity_on_hand", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityOnHand;

    @Column(name = "minimum_threshold", nullable = false, precision = 12, scale = 3)
    private BigDecimal minimumThreshold;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BranchJpaEntity getBranch() {
        return branch;
    }

    public void setBranch(BranchJpaEntity branch) {
        this.branch = branch;
    }

    public MenuItemJpaEntity getMenuItem() {
        return menuItem;
    }

    public void setMenuItem(MenuItemJpaEntity menuItem) {
        this.menuItem = menuItem;
    }

    public BigDecimal getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(BigDecimal quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public BigDecimal getMinimumThreshold() {
        return minimumThreshold;
    }

    public void setMinimumThreshold(BigDecimal minimumThreshold) {
        this.minimumThreshold = minimumThreshold;
    }

}
