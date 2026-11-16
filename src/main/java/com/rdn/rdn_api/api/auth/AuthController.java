package com.rdn.rdn_api.api.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.application.auth.LoginCommand;
import com.rdn.rdn_api.application.auth.LoginService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints de autenticacao.
 *
 * <p>O controller so traduz HTTP para o caso de uso: nenhuma regra de senha ou
 * token mora aqui.</p>
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Login e identidade do usuario autenticado")
public class AuthController {

    private final LoginService loginService;

    public AuthController(LoginService loginService) {
        this.loginService = loginService;
    }

    /**
     * Autentica o usuario e devolve o token de acesso.
     *
     * @param request e-mail e senha
     * @return token e dados publicos do usuario
     */
    @PostMapping("/login")
    @Operation(summary = "Autentica o usuario e emite JWT", security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token emitido"),
            @ApiResponse(responseCode = "400", description = "Corpo invalido"),
            @ApiResponse(responseCode = "401", description = "Credenciais invalidas"),
            @ApiResponse(responseCode = "403", description = "Usuario inativo")
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return LoginResponse.from(loginService.login(new LoginCommand(request.email(), request.senha())));
    }

    /**
     * Devolve o usuario dono do token enviado.
     *
     * <p>Serve como prova viva do filtro JWT: sem {@code Authorization: Bearer}
     * valido a requisicao nem chega ao metodo.</p>
     *
     * @param principal identidade resolvida pelo filtro de seguranca
     * @return dados publicos do usuario autenticado
     */
    @GetMapping("/me")
    @Operation(summary = "Retorna o usuario dono do token")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario autenticado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
    })
    public LoginResponse.UserSummary me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return LoginResponse.UserSummary.from(principal);
    }
}
