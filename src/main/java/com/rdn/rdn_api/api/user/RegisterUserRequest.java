package com.rdn.rdn_api.api.user;

import com.rdn.rdn_api.domain.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de {@code POST /usuarios}.
 *
 * @param nome   nome completo
 * @param email  e-mail do novo usuario
 * @param senha  senha em texto puro, entre 8 e 72 caracteres
 * @param perfil perfil de acesso desejado
 */
public record RegisterUserRequest(
        @NotBlank(message = "nome e obrigatorio")
        @Size(max = 120, message = "nome deve ter no maximo 120 caracteres")
        String nome,

        @NotBlank(message = "email e obrigatorio")
        @Email(message = "email em formato invalido")
        @Size(max = 160, message = "email deve ter no maximo 160 caracteres")
        String email,

        @NotBlank(message = "senha e obrigatoria")
        @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
        String senha,

        @NotNull(message = "perfil e obrigatorio")
        UserRole perfil) {
}
