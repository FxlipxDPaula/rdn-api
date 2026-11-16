package com.rdn.rdn_api.api.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.application.user.RegisterUserCommand;
import com.rdn.rdn_api.application.user.RegisterUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Cadastro de usuarios.
 *
 * <p>Restrito a administracao: o controller apenas traduz HTTP em comando e
 * devolve o recorte publico do usuario.</p>
 */
@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuarios", description = "Cadastro de usuarios restrito a ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final RegisterUserService registerUserService;

    public UserController(RegisterUserService registerUserService) {
        this.registerUserService = registerUserService;
    }

    /**
     * Cadastra um usuario com senha hasheada.
     *
     * @param request   nome, e-mail, senha e perfil
     * @param principal administrador autenticado
     * @return usuario criado, sem expor o hash
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um usuario com senha hasheada")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario criado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao"),
            @ApiResponse(responseCode = "409", description = "E-mail ja cadastrado")
    })
    public UserResponse create(@Valid @RequestBody RegisterUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        RegisterUserCommand command = new RegisterUserCommand(request.nome(), request.email(), request.senha(),
                request.perfil());

        return UserResponse.from(registerUserService.register(command, principal));
    }
}
