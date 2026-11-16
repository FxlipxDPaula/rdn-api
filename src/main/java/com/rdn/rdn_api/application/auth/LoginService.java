package com.rdn.rdn_api.application.auth;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.rdn.rdn_api.application.audit.AuditAction;
import com.rdn.rdn_api.application.audit.AuditEntry;
import com.rdn.rdn_api.application.audit.AuditGateway;
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

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final AuthUserGateway userGateway;

    private final PasswordEncoder passwordEncoder;

    private final TokenIssuer tokenIssuer;

    private final AuditGateway auditGateway;

    public LoginService(AuthUserGateway userGateway, PasswordEncoder passwordEncoder, TokenIssuer tokenIssuer,
            AuditGateway auditGateway) {
        this.userGateway = userGateway;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.auditGateway = auditGateway;
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

        User user = userGateway.findByEmail(email).orElse(null);
        if (user == null || !passwordMatches(command.password(), user.getPasswordHash())) {
            auditLoginFailure(email, "credenciais invalidas");
            throw new InvalidCredentialsException("E-mail ou senha invalidos.");
        }

        if (!user.isActive()) {
            auditLoginFailure(email, "usuario inativo");
            throw new InactiveUserException("Usuario inativo.");
        }

        TokenIssuer.IssuedToken token = tokenIssuer.issue(user);
        auditLoginSuccess(user);

        return new LoginResult(token.accessToken(), "Bearer", token.expiresInSeconds(), AuthenticatedUser.from(user));
    }

    private void auditLoginSuccess(User user) {
        recordAuditSafely(new AuditEntry(user.getId(), user.getEmail(), user.getRole(),
                AuditAction.LOGIN_SUCCESS, "AUTH", user.getId(), "login"));
    }

    private void auditLoginFailure(String email, String reason) {
        recordAuditSafely(new AuditEntry(null, email, null, AuditAction.LOGIN_FAILURE, "AUTH", null, reason));
    }

    private void recordAuditSafely(AuditEntry entry) {
        try {
            auditGateway.record(entry);
        } catch (RuntimeException ex) {
            log.warn("Falha ao registrar auditoria de login: {}", ex.getMessage());
        }
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
