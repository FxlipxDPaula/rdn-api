package com.rdn.rdn_api.application.loyalty.exception;

import java.math.BigDecimal;

/**
 * Levantada quando o desconto do resgate ultrapassa o saldo devedor do pedido.
 */
public class RedemptionExceedsOrderTotalException extends RuntimeException {

    private final BigDecimal discount;

    private final BigDecimal remaining;

    public RedemptionExceedsOrderTotalException(BigDecimal discount, BigDecimal remaining) {
        super("Resgate maior que o saldo devedor do pedido.");
        this.discount = discount;
        this.remaining = remaining;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }
}
