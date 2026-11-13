package com.rdn.rdn_api.application.order.exception;

/**
 * Operacao negada ao perfil autenticado (ex.: cliente tentando alterar pedido
 * de outro cliente).
 *
 * <p>Traduzida para {@code 403 ACESSO_NEGADO}.</p>
 */
public class OperationNotAllowedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OperationNotAllowedException(String message) {
        super(message);
    }
}
