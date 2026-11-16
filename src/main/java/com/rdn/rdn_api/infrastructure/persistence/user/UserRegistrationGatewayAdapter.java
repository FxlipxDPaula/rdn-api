package com.rdn.rdn_api.infrastructure.persistence.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.rdn.rdn_api.application.user.UserRegistrationGateway;
import com.rdn.rdn_api.application.user.exception.EmailAlreadyRegisteredException;
import com.rdn.rdn_api.domain.User;
import com.rdn.rdn_api.infrastructure.persistence.entity.UserJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.mapper.UserMapper;
import com.rdn.rdn_api.infrastructure.repository.UserJpaRepository;

/**
 * Adaptador da porta {@link UserRegistrationGateway} sobre o repositorio JPA.
 *
 * <p>O {@code saveAndFlush} forca a violacao de unicidade a aparecer dentro do
 * adaptador, permitindo traduzir a corrida de e-mails duplicados para a
 * excecao de aplicacao.</p>
 */
@Component
public class UserRegistrationGatewayAdapter implements UserRegistrationGateway {

    private final UserJpaRepository userRepository;

    public UserRegistrationGatewayAdapter(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserMapper.toEntity(user);

        try {
            return UserMapper.toDomain(userRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyRegisteredException("Ja existe usuario cadastrado com este e-mail.");
        }
    }
}
