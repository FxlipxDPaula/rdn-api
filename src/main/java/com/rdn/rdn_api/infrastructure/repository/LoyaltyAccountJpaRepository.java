package com.rdn.rdn_api.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rdn.rdn_api.infrastructure.persistence.entity.LoyaltyAccountJpaEntity;

import jakarta.persistence.LockModeType;

/**
 * Acesso a tabela {@code loyalty_accounts}.
 */
public interface LoyaltyAccountJpaRepository extends JpaRepository<LoyaltyAccountJpaEntity, UUID> {

    Optional<LoyaltyAccountJpaEntity> findByCustomerId(UUID customerId);

    /**
     * Le a conta com lock pessimista de escrita.
     *
     * <p>Usado no resgate: duas requisicoes simultaneas nao podem debitar o
     * mesmo saldo duas vezes.</p>
     *
     * @param customerId cliente
     * @return conta travada ate o fim da transacao
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LoyaltyAccountJpaEntity a where a.customer.id = :customerId")
    Optional<LoyaltyAccountJpaEntity> findForUpdateByCustomerId(@Param("customerId") UUID customerId);

}
