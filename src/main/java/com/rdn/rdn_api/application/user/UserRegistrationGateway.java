package com.rdn.rdn_api.application.user;

import com.rdn.rdn_api.domain.User;

/**
 * Porta de saida do cadastro de usuarios.
 *
 * <p>Mantem a camada de aplicacao independente de JPA; o adaptador concreto
 * fica em {@code infrastructure/persistence/user}.</p>
 */
public interface UserRegistrationGateway {

    /**
     * Verifica se ja existe usuario com o e-mail informado.
     *
     * @param email e-mail normalizado
     * @return {@code true} quando o e-mail ja esta cadastrado
     */
    boolean existsByEmail(String email);

    /**
     * Persiste um novo usuario.
     *
     * @param user usuario de dominio com hash de senha
     * @return usuario persistido, com identificador gerado
     */
    User save(User user);
}
