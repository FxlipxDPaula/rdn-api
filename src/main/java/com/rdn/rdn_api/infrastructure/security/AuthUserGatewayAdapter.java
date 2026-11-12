package com.rdn.rdn_api.infrastructure.security;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.rdn.rdn_api.application.auth.AuthUserGateway;
import com.rdn.rdn_api.domain.User;
import com.rdn.rdn_api.infrastructure.persistence.mapper.UserMapper;
import com.rdn.rdn_api.infrastructure.repository.UserJpaRepository;

/**
 * Adaptador da porta {@link AuthUserGateway} sobre o repositorio JPA.
 *
 * <p>Faz a ponte entre a entidade persistente e o {@link User} de dominio
 * usando {@link UserMapper}, mantendo a camada de aplicacao ignorante de JPA.</p>
 */
@Component
public class AuthUserGatewayAdapter implements AuthUserGateway {

    private final UserJpaRepository userRepository;

    public AuthUserGatewayAdapter(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email).map(UserMapper::toDomain);
    }
}
