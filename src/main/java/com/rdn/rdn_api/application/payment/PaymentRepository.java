package com.rdn.rdn_api.application.payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.rdn.rdn_api.domain.Payment;

/**
 * Porta de saida para persistencia das tentativas de pagamento.
 */
public interface PaymentRepository {

    /**
     * Persiste a tentativa.
     *
     * @param payment pagamento de dominio
     * @return pagamento persistido
     */
    Payment save(Payment payment);

    /**
     * Busca a tentativa pela chave de idempotencia.
     *
     * @param idempotencyKey chave enviada pelo cliente
     * @return tentativa existente, ou vazio
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * Lista o historico de tentativas de um pedido, da mais recente para a mais antiga.
     *
     * @param orderId identificador do pedido
     * @return tentativas do pedido
     */
    List<Payment> findByOrderId(UUID orderId);
}
