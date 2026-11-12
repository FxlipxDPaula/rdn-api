package com.rdn.rdn_api.application.auth;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.rdn.rdn_api.domain.User;

/**
 * Caso de uso de login.
 *
 * <p>Orquestra tres passos: localizar o usuario pela porta
 * {@link AuthUserGateway}, comparar a senha informada com o hash armazenado
 * usando {@link PasswordEncoder} e emitir o token via {@link TokenIssuer}.</p>
 *
 * <p>A senha em texto puro so existe durante a execucao deste metodo. O hash
 * nunca sai do servidor: {@link AuthenticatedUser} e o recorte devolvido.</p>
 */
@Service
public class LoginService {

    private final AuthUserGateway userGateway;

    private final PasswordEncoder passwordEncoder;

    private final TokenIssuer tokenIssuer;

    public LoginService(AuthUserGateway userGateway, PasswordEncoder passwordEncoder, TokenIssuer tokenIssuer) {
        this.userGateway = userGateway;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
    }

    /**
     * Autentica o usuario e devolve o token de acesso.
     *
     * @param command e-mail e senha informados
     * @return token e identidade autenticada
     * @throws InvalidCredentialsException quando o e-mail nao existe ou a senha
     *                                     nao confere
     * @throws InactiveUserException       quando as credenciais conferem mas o
     *                                     usuario esta desativado
     */
    public LoginResult login(LoginCommand command) {
        String email = normalizeEmail(command.email());

        User user = userGateway.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("E-mail ou senha invalidos."));

        if (!passwordMatches(command.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("E-mail ou senha invalidos.");
        }

        if (!user.isActive()) {
            throw new InactiveUserException("Usuario inativo.");
        }

        TokenIssuer.IssuedToken token = tokenIssuer.issue(user);
        return new LoginResult(token.accessToken(), "Bearer", token.expiresInSeconds(), AuthenticatedUser.from(user));
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean passwordMatches(String rawPassword, String storedHash) {
        if (rawPassword == null || rawPassword.isBlank() || storedHash == null) {
            return false;
        }

        return passwordEncoder.matches(rawPassword, storedHash);
    }
}
