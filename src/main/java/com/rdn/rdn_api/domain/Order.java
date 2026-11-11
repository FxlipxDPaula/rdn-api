package com.rdn.rdn_api.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pedido de uma filial.
 *
 * <p>POJO de dominio: campos, construtor padrao, getters/setters e as regras
 * que protegem os invariantes do pedido. Nao contem anotacoes de persistencia.
 * As unicas excecoes lancadas sao de invariante violado
 * ({@link IllegalArgumentException} para entrada invalida e
 * {@link IllegalStateException} para transicao de status nao permitida).</p>
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

    private String idempotencyKey;

    /**
     * Transicoes de status permitidas. {@link OrderStatus#DELIVERED} e
     * {@link OrderStatus#CANCELLED} sao estados finais e nao possuem saida.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS =
            Map.of(
                    OrderStatus.RECEIVED, Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
                    OrderStatus.PREPARING, Set.of(OrderStatus.READY, OrderStatus.CANCELLED),
                    OrderStatus.READY, Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED),
                    OrderStatus.DELIVERED, Set.of(),
                    OrderStatus.CANCELLED, Set.of());

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
        if (discount.signum() < 0) {
            discount = BigDecimal.ZERO;
        }
        if (discount.compareTo(sum) > 0) {
            discount = sum;
        }

        totalAmount = sum.subtract(discount);
        return totalAmount;
    }

    /**
     * Aplica a transicao de status validando a maquina de estados do pedido.
     *
     * <p>Fluxo permitido: {@code RECEIVED -> PREPARING -> READY -> DELIVERED}.
     * A partir de qualquer estado nao final tambem e permitido cancelar. Um
     * pedido ainda sem status so pode ser iniciado em
     * {@link OrderStatus#RECEIVED}.</p>
     *
     * @param newStatus nova situacao
     * @throws IllegalArgumentException quando {@code newStatus} for nulo
     * @throws IllegalStateException    quando a transicao nao for permitida
     */
    public void changeStatus(OrderStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("status e obrigatorio");
        }

        if (status == null) {
            if (OrderStatus.RECEIVED != newStatus) {
                throw new IllegalStateException("pedido deve iniciar em RECEIVED");
            }
            this.status = newStatus;
            return;
        }

        if (!ALLOWED_TRANSITIONS.getOrDefault(status, Set.of()).contains(newStatus)) {
            throw new IllegalStateException(
                    "transicao de " + status + " para " + newStatus + " nao permitida");
        }

        this.status = newStatus;
    }

    /**
     * Cancela o pedido registrando o motivo informado.
     *
     * @param reason motivo do cancelamento
     * @throws IllegalStateException quando o pedido ja estiver em estado final
     */
    public void cancel(String reason) {
        changeStatus(OrderStatus.CANCELLED);
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

}
