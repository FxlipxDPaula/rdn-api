package com.rdn.rdn_api.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Movimentacao de estoque.
 *
 * <p>POJO de dominio: apenas campos, construtor padrao e getters/setters. Nao
 * contem anotacoes de persistencia nem regras de negocio.</p>
 */
public class InventoryMovement {

    private UUID id;

    private InventoryItem inventoryItem;

    private MovementType type;

    private BigDecimal quantity;

    private LocalDateTime timestamp;

    private String notes;

    public InventoryMovement() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public MovementType getType() {
        return type;
    }

    public void setType(MovementType type) {
        this.type = type;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

}
