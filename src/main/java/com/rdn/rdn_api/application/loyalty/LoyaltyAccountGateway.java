package com.rdn.rdn_api.application.loyalty;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;

import com.rdn.rdn_api.domain.LoyaltyAccount;
import com.rdn.rdn_api.domain.LoyaltyTransaction;
import com.rdn.rdn_api.domain.TransactionType;

/**
 * Porta de saida da conta de fidelidade e do seu extrato.
 */
public interface LoyaltyAccountGateway {

    /**
     * Busca a conta pelo cliente.
     *
     * @param customerId cliente
     * @return conta, ou vazio quando o cliente ainda nao consentiu
     */
    Optional<LoyaltyAccount> findByCustomerId(UUID customerId);

    /**
     * Busca a conta com lock pessimista para debito de pontos.
     *
     * @param customerId cliente
     * @return conta travada ate o fim da transacao
     */
    Optional<LoyaltyAccount> findForUpdateByCustomerId(UUID customerId);

    /**
     * Persiste a conta (insercao quando o id e nulo).
     *
     * @param account conta de dominio
     * @return conta persistida
     */
    LoyaltyAccount saveAccount(LoyaltyAccount account);

    /**
     * Persiste um lancamento do extrato.
     *
     * @param transaction lancamento
     * @return lancamento persistido
     */
    LoyaltyTransaction saveTransaction(LoyaltyTransaction transaction);

    /**
     * Lista o extrato do cliente, do mais recente para o mais antigo.
     *
     * @param customerId cliente
     * @param page       pagina (base zero)
     * @param size       tamanho
     * @return pagina de lancamentos; vazia quando nao ha conta
     */
    Page<LoyaltyTransaction> findTransactions(UUID customerId, int page, int size);

    /**
     * Informa se o pedido ja possui lancamento do tipo informado.
     *
     * @param orderId pedido
     * @param type    tipo do lancamento
     * @return {@code true} quando ja existe
     */
    boolean existsTransaction(UUID orderId, TransactionType type);
}
