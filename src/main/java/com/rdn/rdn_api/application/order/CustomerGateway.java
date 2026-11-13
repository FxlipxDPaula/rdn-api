package com.rdn.rdn_api.application.order;

import java.util.Optional;
import java.util.UUID;

import com.rdn.rdn_api.domain.User;

/**
 * Porta de saida para localizar o cliente de um pedido.
 */
public interface CustomerGateway {

    /**
     * Busca o usuario pelo identificador.
     *
     * @param id identificador do usuario
     * @return usuario, ou vazio quando nao existe
     */
    Optional<User> findById(UUID id);
}
