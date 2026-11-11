package com.rdn.rdn_api.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rdn.rdn_api.infrastructure.persistence.entity.PaymentJpaEntity;

/**
 * Acesso a tabela {@code payments}.
 */
public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    List<PaymentJpaEntity> findByOrderIdOrderByRequestedAtDesc(UUID orderId);

    Optional<PaymentJpaEntity> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

}
