package com.rdn.rdn_api.infrastructure.persistence.mapper;

import com.rdn.rdn_api.domain.AuthSession;
import com.rdn.rdn_api.infrastructure.persistence.entity.AuthSessionJpaEntity;

/**
 * Conversao entre {@link AuthSession} e {@link AuthSessionJpaEntity}.
 *
 * <p>O token e persistido somente como hash, portanto nao e possivel
 * reconstruir o valor original: {@code toDomain} devolve o token nulo e a
 * validacao de sessao deve comparar o hash recebido com o armazenado.</p>
 */
public final class AuthSessionMapper {

    private AuthSessionMapper() {
    }

    public static AuthSession toDomain(AuthSessionJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        AuthSession session = new AuthSession();
        session.setUser(UserMapper.toDomain(entity.getUser()));
        session.setIssuedAt(entity.getIssuedAt());
        session.setExpiresAt(entity.getExpiresAt());

        return session;
    }

    public static void copyScalarsToEntity(AuthSession domain, AuthSessionJpaEntity entity) {
        entity.setIssuedAt(domain.getIssuedAt());
        entity.setExpiresAt(domain.getExpiresAt());
    }

}
