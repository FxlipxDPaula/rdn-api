package com.rdn.rdn_api.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rdn.rdn_api.infrastructure.persistence.entity.UserJpaEntity;

/**
 * Acesso a tabela {@code users}.
 */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

}
