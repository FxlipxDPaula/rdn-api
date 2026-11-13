package com.rdn.rdn_api.infrastructure.persistence.order;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.application.order.CustomerGateway;
import com.rdn.rdn_api.domain.User;
import com.rdn.rdn_api.infrastructure.persistence.mapper.UserMapper;
import com.rdn.rdn_api.infrastructure.repository.UserJpaRepository;

/**
 * Adaptador de {@link CustomerGateway} sobre {@code users}.
 */
@Component
public class CustomerGatewayAdapter implements CustomerGateway {

    private final UserJpaRepository userRepository;

    public CustomerGatewayAdapter(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id).map(UserMapper::toDomain);
    }
}
