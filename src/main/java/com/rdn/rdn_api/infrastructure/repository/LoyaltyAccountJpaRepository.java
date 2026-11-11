package com.rdn.rdn_api.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rdn.rdn_api.infrastructure.persistence.entity.LoyaltyAccountJpaEntity;

/**
 * Acesso a tabela {@code loyalty_accounts}.
 */
public interface LoyaltyAccountJpaRepository extends JpaRepository<LoyaltyAccountJpaEntity, UUID> {

    Optional<LoyaltyAccountJpaEntity> findByCustomerId(UUID customerId);

}
