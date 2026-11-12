package com.rdn.rdn_api.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Corpo de {@code POST /auth/login}.
 *
 * <p>O nome {@code senha} segue o contrato do roteiro da disciplina; dentro da
 * aplicacao o campo vira {@code password} em {@code LoginCommand}.</p>
 *
 * @param email e-mail do usuario
 * @param senha senha em texto puro
 */
public record LoginRequest(
        @NotBlank(message = "email e obrigatorio")
        @Email(message = "email em formato invalido")
        String email,

        @NotBlank(message = "senha e obrigatoria")
        String senha) {
}
