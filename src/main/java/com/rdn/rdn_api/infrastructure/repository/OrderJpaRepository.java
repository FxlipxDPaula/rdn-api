package com.rdn.rdn_api.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.SalesChannel;
import com.rdn.rdn_api.infrastructure.persistence.entity.OrderJpaEntity;

/**
 * Acesso a tabela {@code orders}.
 *
 * <p>As consultas por canal e por canal + status sustentam o filtro
 * {@code ?canalPedido=} exigido pelo roteiro.</p>
 */
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    Page<OrderJpaEntity> findByOriginChannel(SalesChannel originChannel, Pageable pageable);

    Page<OrderJpaEntity> findByOriginChannelAndStatus(SalesChannel originChannel,
            OrderStatus status, Pageable pageable);

    Optional<OrderJpaEntity> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {"items", "items.menuItem", "customer", "branch"})
    Optional<OrderJpaEntity> findWithItemsById(UUID id);

}
