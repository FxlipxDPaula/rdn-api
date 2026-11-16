package com.rdn.rdn_api.infrastructure.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.rdn.rdn_api.domain.TransactionType;
import com.rdn.rdn_api.infrastructure.persistence.entity.LoyaltyTransactionJpaEntity;

/**
 * Acesso a tabela {@code loyalty_transactions}.
 */
public interface LoyaltyTransactionJpaRepository
        extends JpaRepository<LoyaltyTransactionJpaEntity, UUID> {

    List<LoyaltyTransactionJpaEntity> findByLoyaltyAccountIdOrderByCreatedAtDesc(
            UUID loyaltyAccountId);

    Page<LoyaltyTransactionJpaEntity> findByLoyaltyAccountIdOrderByCreatedAtDesc(
            UUID loyaltyAccountId, Pageable pageable);

    boolean existsByOrderIdAndType(UUID orderId, TransactionType type);

}
