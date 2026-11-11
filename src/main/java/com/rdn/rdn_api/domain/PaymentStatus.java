package com.rdn.rdn_api.domain;

/**
 * Situacao de um pagamento.
 *
 * <p>Enum simples de dominio (POJO), sem atributos ou regras de negocio. O
 * estado inicial de um pagamento criado e {@link #PENDING}; {@link #APPROVED},
 * {@link #DECLINED} e {@link #FAILED} sao estados finais.</p>
 */
public enum PaymentStatus {

    PENDING,
    APPROVED,
    DECLINED,
    FAILED

}
