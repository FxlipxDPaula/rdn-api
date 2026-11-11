package com.rdn.rdn_api.infrastructure.persistence.mapper;

import com.rdn.rdn_api.domain.User;
import com.rdn.rdn_api.infrastructure.persistence.entity.UserJpaEntity;

/**
 * Conversao entre {@link User} e {@link UserJpaEntity}.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        User user = new User();
        user.setId(entity.getId());
        user.setFullName(entity.getFullName());
        user.setEmail(entity.getEmail());
        user.setPasswordHash(entity.getPasswordHash());
        user.setRole(entity.getRole());
        user.setIsActive(entity.getIsActive());

        return user;
    }

    public static UserJpaEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }

        UserJpaEntity entity = new UserJpaEntity();
        copyScalarsToEntity(domain, entity);

        return entity;
    }

    public static void copyScalarsToEntity(User domain, UserJpaEntity entity) {
        entity.setId(domain.getId());
        entity.setFullName(domain.getFullName());
        entity.setEmail(domain.getEmail());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setRole(domain.getRole());
        entity.setIsActive(domain.getIsActive());
    }

}
