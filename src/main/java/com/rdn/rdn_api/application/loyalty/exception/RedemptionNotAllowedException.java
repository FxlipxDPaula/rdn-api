package com.rdn.rdn_api.application.loyalty.exception;

import java.util.UUID;

import com.rdn.rdn_api.domain.OrderStatus;

/**
 * Levantada quando o pedido nao esta em um status que aceite resgate.
 */
public class RedemptionNotAllowedException extends RuntimeException {

    public RedemptionNotAllowedException(UUID pedidoId, OrderStatus status) {
        super("Pedido " + pedidoId + " esta em " + status + " e nao aceita resgate.");
    }
}
