package com.rdn.rdn_api.domain;

/**
 * Natureza de uma movimentacao de estoque.
 *
 * <p>Enum simples de dominio (POJO), sem atributos ou regras de negocio.</p>
 */
public enum MovementType {

    INBOUND,
    OUTBOUND,
    LOSS,
    ADJUSTMENT

}
