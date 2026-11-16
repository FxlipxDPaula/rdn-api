package com.rdn.rdn_api.application.user;

import com.rdn.rdn_api.domain.UserRole;

/**
 * Dados necessarios para cadastrar um usuario.
 *
 * <p>A senha chega em texto puro e existe apenas durante a execucao do caso de
 * uso; o hash gerado e o unico valor persistido.</p>
 *
 * @param fullName    nome completo
 * @param email       e-mail informado
 * @param rawPassword senha em texto puro
 * @param role        perfil de acesso desejado
 */
public record RegisterUserCommand(String fullName, String email, String rawPassword, UserRole role) {
}
