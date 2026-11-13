package com.rdn.rdn_api.application.payment.exception;

import java.util.UUID;

import com.rdn.rdn_api.domain.OrderStatus;

/**
 * Pedido nao esta no estado que aceita cobranca.
 *
 * <p>Traduzida para {@code 409 PEDIDO_NAO_AGUARDA_PAGAMENTO}.</p>
 */
public class OrderNotAwaitingPaymentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient UUID pedidoId;

    private final transient OrderStatus statusAtual;

    public OrderNotAwaitingPaymentException(UUID pedidoId, OrderStatus statusAtual) {
        super("Pedido " + pedidoId + " esta em " + statusAtual + " e nao aguarda pagamento");
        this.pedidoId = pedidoId;
        this.statusAtual = statusAtual;
    }

    public UUID getPedidoId() {
        return pedidoId;
    }

    public OrderStatus getStatusAtual() {
        return statusAtual;
    }
}
