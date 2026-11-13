package com.rdn.rdn_api.application.order.exception;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Saldo de estoque insuficiente para atender o pedido.
 *
 * <p>Carrega o produto, o saldo disponivel e a quantidade pedida para que a
 * borda HTTP monte o campo {@code details} do erro padrao. Traduzida para
 * {@code 409 ESTOQUE_INSUFICIENTE}.</p>
 */
public class InsufficientStockException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient UUID produtoId;

    private final transient BigDecimal disponivel;

    private final transient BigDecimal solicitado;

    public InsufficientStockException(UUID produtoId, BigDecimal disponivel, BigDecimal solicitado) {
        super("Estoque insuficiente para o produto " + produtoId);
        this.produtoId = produtoId;
        this.disponivel = disponivel;
        this.solicitado = solicitado;
    }

    public UUID getProdutoId() {
        return produtoId;
    }

    public BigDecimal getDisponivel() {
        return disponivel;
    }

    public BigDecimal getSolicitado() {
        return solicitado;
    }
}
