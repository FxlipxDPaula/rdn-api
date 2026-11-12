package com.rdn.rdn_api.application.auth;

import java.util.Optional;

import com.rdn.rdn_api.domain.User;

/**
 * Porta de leitura de usuarios usada pelo caso de uso de login.
 *
 * <p>Mantem a camada de aplicacao independente de JPA: o adaptador concreto
 * fica em {@code infrastructure/security}.</p>
 */
public interface AuthUserGateway {

    /**
     * Busca um usuario pelo e-mail.
     *
     * @param email e-mail ja normalizado
     * @return usuario encontrado, ou vazio quando nao existe
     */
    Optional<User> findByEmail(String email);
}
