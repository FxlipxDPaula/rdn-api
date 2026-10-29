package com.rdn.rdn_api.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pedido de uma filial.
 *
 * <p>POJO de dominio: apenas campos, construtor padrao, getters/setters e
 * operacoes triviais de manipulacao da lista de itens. Nao contem anotacoes de
 * persistencia nem regras de negocio (sem validacoes, sem invariantes e sem
 * excecoes de dominio).</p>
 *
 * <p>Premissa assumida: {@link OrderItem} expoe o construtor
 * {@code OrderItem(MenuItem, Integer)}, o getter {@code getId()} e o metodo
 * {@code calculateSubtotal()}.</p>
 */
public class Order {

    private UUID id;

    private User customer;

    private Branch branch;

    private SalesChannel originChannel;

    private OrderStatus status;

    private BigDecimal totalAmount = BigDecimal.ZERO;

    private BigDecimal discountAmount = BigDecimal.ZERO;

    private LocalDateTime createdAt;

    private List<OrderItem> items = new ArrayList<>();

    private String cancellationReason;

    public Order() {
    }

    /**
     * Adiciona um novo item ao pedido.
     *
     * @param item     produto do cardapio
     * @param quantity quantidade desejada
     */
    public void addItem(MenuItem item, Integer quantity) {
        OrderItem orderItem = new OrderItem(item, quantity);
        if (item != null) {
            orderItem.setUnitPrice(item.getPrice());
        }

        items.add(orderItem);
    }

    /**
     * Remove do pedido o item cujo identificador corresponde ao informado.
     * Nao lanca excecao quando o item nao existe.
     *
     * @param orderItemId identificador do item do pedido
     */
    public void removeItem(UUID orderItemId) {
        items.removeIf(orderItem -> orderItem.getId().equals(orderItemId));
    }

    /**
     * Calcula o total do pedido como a soma dos subtotais dos itens menos o
     * desconto, atualizando {@code totalAmount}.
     *
     * @return total calculado, ou {@link BigDecimal#ZERO} quando nao ha itens
     */
    public BigDecimal calculateTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderItem orderItem : items) {
            sum = sum.add(orderItem.calculateSubtotal());
        }

        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        totalAmount = sum.subtract(discount);
        return totalAmount;
    }

    /**
     * Atualiza a situacao do pedido.
     *
     * @param newStatus nova situacao
     */
    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * Cancela o pedido registrando o motivo informado.
     *
     * @param reason motivo do cancelamento
     */
    public void cancel(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.cancellationReason = reason;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public SalesChannel getOriginChannel() {
        return originChannel;
    }

    public void setOriginChannel(SalesChannel originChannel) {
        this.originChannel = originChannel;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

}
