package com.rdn.rdn_api.application.promotion.exception;

/**
 * Levantada quando a campanha existe mas nao vale para o pedido.
 */
public class PromotionNotApplicableException extends RuntimeException {

    public PromotionNotApplicableException(String reason) {
        super(reason);
    }
}
