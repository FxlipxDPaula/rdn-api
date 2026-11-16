package com.rdn.rdn_api.application.loyalty.exception;

import java.util.UUID;

/**
 * Levantada quando o pedido ja possui um resgate de pontos.
 */
public class RedemptionAlreadyAppliedException extends RuntimeException {

    public RedemptionAlreadyAppliedException(UUID pedidoId) {
        super("Pedido " + pedidoId + " ja possui resgate de pontos.");
    }
}
