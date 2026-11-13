package com.rdn.rdn_api.application.payment;

/**
 * Porta de saida para o provedor externo de pagamento.
 *
 * <p>Trocar o mock por um provedor real significa implementar esta interface na
 * infraestrutura; nenhum caso de uso muda.</p>
 */
public interface PaymentProvider {

    /**
     * Envia a cobranca e devolve o desfecho.
     *
     * @param request dados da cobranca
     * @return resultado do provedor
     */
    ChargeResult charge(ChargeRequest request);
}
