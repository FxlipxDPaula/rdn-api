package com.rdn.rdn_api.infrastructure.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rdn.rdn_api.infrastructure.persistence.entity.PromotionJpaEntity;

/**
 * Acesso a tabela {@code promotions}.
 */
public interface PromotionJpaRepository extends JpaRepository<PromotionJpaEntity, UUID> {

    List<PromotionJpaEntity> findByIsActiveTrue();

}
