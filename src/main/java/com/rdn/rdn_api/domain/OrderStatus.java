package com.rdn.rdn_api.domain;

/**
 * Situacao do pedido.
 *
 * <p>Enum simples de dominio (POJO), sem mapa de transicoes nem metodos
 * auxiliares.</p>
 */
public enum OrderStatus {

    RECEIVED,
    PREPARING,
    READY,
    DELIVERED,
    CANCELLED

}
