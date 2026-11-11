package com.rdn.rdn_api.domain;

/**
 * Forma de pagamento escolhida no pedido.
 *
 * <p>Enum simples de dominio (POJO), sem atributos ou regras de negocio.
 * {@link #MOCK} representa o provedor externo simulado exigido pelo roteiro do
 * projeto; os demais valores existem para manter o contrato preparado para
 * meios de pagamento reais.</p>
 */
public enum PaymentMethod {

    MOCK,
    CASH,
    CARD,
    PIX

}
