package com.rdn.rdn_api.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Promocao aplicavel a pedidos.
 *
 * <p>POJO de dominio: campos, construtor padrao, getters/setters e calculos
 * simplificados de vigencia e desconto, sempre null-safe. Nao contem anotacoes
 * de persistencia nem validacoes com excecoes.</p>
 */
public class Promotion {

    private UUID id;

    private String title;

    private String description;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private BigDecimal minOrderValue;

    private Boolean isActive;

    public Promotion() {
    }

    /**
     * Informa se a promocao esta vigente para o pedido.
     *
     * <p>Considera a flag {@code isActive}, a janela entre {@code startDate} e
     * {@code endDate} (comparada com o instante atual) e o valor minimo do
     * pedido.</p>
     *
     * @param order pedido a ser avaliado
     * @return {@code true} quando a promocao pode ser aplicada
     */
    public Boolean isValidFor(Order order) {
        BigDecimal orderValue = order == null ? BigDecimal.ZERO : order.subtotal();
        boolean reachesMinimum = minOrderValue == null
                || orderValue.compareTo(minOrderValue) >= 0;

        return Boolean.TRUE.equals(isActive)
                && isWithinValidity(LocalDateTime.now())
                && reachesMinimum;
    }

    /**
     * Informa se a janela de vigencia cobre o instante informado.
     *
     * <p>Separado de {@link #isValidFor(Order)} para a listagem de campanhas poder
     * exibir a vigencia sem precisar montar um pedido.</p>
     *
     * @param moment instante de referencia
     * @return {@code true} quando a promocao esta dentro do periodo
     */
    public boolean isWithinValidity(LocalDateTime moment) {
        LocalDateTime reference = moment == null ? LocalDateTime.now() : moment;

        boolean started = startDate == null || !reference.isBefore(startDate);
        boolean notEnded = endDate == null || !reference.isAfter(endDate);

        return started && notEnded;
    }

    /**
     * Calcula o valor do desconto a aplicar sobre o pedido.
     *
     * @param order pedido alvo do desconto
     * @return valor do desconto, ou {@link BigDecimal#ZERO} quando a promocao
     *         nao e valida ou nao ha {@code discountValue}
     */
    public BigDecimal applyDiscount(Order order) {
        if (!Boolean.TRUE.equals(isValidFor(order)) || discountValue == null) {
            return BigDecimal.ZERO;
        }

        if (discountType == DiscountType.PERCENTAGE) {
            BigDecimal orderValue = order == null ? BigDecimal.ZERO : order.subtotal();

            return orderValue.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        return discountValue;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(BigDecimal minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

}
