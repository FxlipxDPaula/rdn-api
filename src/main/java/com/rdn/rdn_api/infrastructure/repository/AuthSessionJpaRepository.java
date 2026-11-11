package com.rdn.rdn_api.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rdn.rdn_api.infrastructure.persistence.entity.AuthSessionJpaEntity;

/**
 * Acesso a tabela {@code auth_sessions}.
 *
 * <p>A busca e feita pelo hash do token, nunca pelo valor em texto puro.</p>
 */
public interface AuthSessionJpaRepository extends JpaRepository<AuthSessionJpaEntity, UUID> {

    Optional<AuthSessionJpaEntity> findByTokenHash(String tokenHash);

    long deleteByExpiresAtBefore(LocalDateTime reference);

}
