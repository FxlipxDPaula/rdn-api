package com.rdn.rdn_api.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.rdn.rdn_api.infrastructure.persistence.entity.MenuJpaEntity;

/**
 * Acesso a tabela {@code menus}.
 */
public interface MenuJpaRepository extends JpaRepository<MenuJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"items", "branch"})
    Optional<MenuJpaEntity> findByBranchId(UUID branchId);

}
