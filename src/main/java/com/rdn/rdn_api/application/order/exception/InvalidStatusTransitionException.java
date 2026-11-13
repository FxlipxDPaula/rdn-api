package com.rdn.rdn_api.application.order.exception;

import java.util.UUID;

import com.rdn.rdn_api.domain.OrderStatus;

/**
 * Transicao de status nao permitida pela maquina de estados do pedido.
 *
 * <p>E a traducao de {@link IllegalStateException} do dominio para uma falha de
 * regra de negocio: {@code 409 TRANSICAO_INVALIDA}.</p>
 */
public class InvalidStatusTransitionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient UUID pedidoId;

    private final transient OrderStatus statusAtual;

    private final transient OrderStatus statusSolicitado;

    public InvalidStatusTransitionException(UUID pedidoId, OrderStatus statusAtual,
            OrderStatus statusSolicitado) {
        super("Transicao de " + statusAtual + " para " + statusSolicitado + " nao permitida");
        this.pedidoId = pedidoId;
        this.statusAtual = statusAtual;
        this.statusSolicitado = statusSolicitado;
    }

    public UUID getPedidoId() {
        return pedidoId;
    }

    public OrderStatus getStatusAtual() {
        return statusAtual;
    }

    public OrderStatus getStatusSolicitado() {
        return statusSolicitado;
    }
}
