package com.rdn.rdn_api.infrastructure.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rdn.rdn_api.infrastructure.persistence.entity.OrderItemJpaEntity;

/**
 * Acesso a tabela {@code order_items}.
 */
public interface OrderItemJpaRepository extends JpaRepository<OrderItemJpaEntity, UUID> {

    List<OrderItemJpaEntity> findByOrderId(UUID orderId);

}
