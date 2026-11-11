package com.rdn.rdn_api.infrastructure.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rdn.rdn_api.infrastructure.persistence.entity.BranchJpaEntity;

/**
 * Acesso a tabela {@code branches}.
 */
public interface BranchJpaRepository extends JpaRepository<BranchJpaEntity, UUID> {

    List<BranchJpaEntity> findByIsOpenTrue();

}
