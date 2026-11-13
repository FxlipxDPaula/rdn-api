package com.rdn.rdn_api.domain;

/**
 * Situacao do pedido.
 *
 * <p>Enum simples de dominio (POJO), sem mapa de transicoes nem metodos
 * auxiliares. O mapa de transicoes permitidas vive em {@link Order}.</p>
 */
public enum OrderStatus {

    /** Pedido aguardando a confirmacao do pagamento mock (Etapa 4). */
    AGUARDANDO_PAGAMENTO,
    /** Pedido pago e na fila da cozinha. */
    RECEIVED,
    PREPARING,
    READY,
    DELIVERED,
    CANCELLED

}
