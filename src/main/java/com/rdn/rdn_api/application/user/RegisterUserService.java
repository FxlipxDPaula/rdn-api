package com.rdn.rdn_api.application.user;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.application.audit.AuditAction;
import com.rdn.rdn_api.application.audit.AuditEntry;
import com.rdn.rdn_api.application.audit.AuditGateway;
import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.application.user.exception.EmailAlreadyRegisteredException;
import com.rdn.rdn_api.domain.User;

/**
 * Caso de uso de cadastro de usuarios.
 *
 * <p>Normaliza o e-mail, recusa duplicidade, gera o hash BCrypt e registra a
 * acao sensivel na auditoria. A senha em texto puro nunca e persistida nem
 * devolvida.</p>
 */
@Service
public class RegisterUserService {

    private static final Logger log = LoggerFactory.getLogger(RegisterUserService.class);

    private final UserRegistrationGateway userGateway;

    private final PasswordEncoder passwordEncoder;

    private final AuditGateway auditGateway;

    public RegisterUserService(UserRegistrationGateway userGateway, PasswordEncoder passwordEncoder,
            AuditGateway auditGateway) {
        this.userGateway = userGateway;
        this.passwordEncoder = passwordEncoder;
        this.auditGateway = auditGateway;
    }

    /**
     * Cadastra um usuario ativo com a senha hasheada.
     *
     * @param command   dados do novo usuario
     * @param principal administrador que executou o cadastro
     * @return usuario persistido
     * @throws EmailAlreadyRegisteredException quando o e-mail ja existe
     */
    @Transactional
    public User register(RegisterUserCommand command, AuthenticatedUser principal) {
        String email = normalizeEmail(command.email());

        if (userGateway.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException("Ja existe usuario cadastrado com este e-mail.");
        }

        User user = new User();
        user.setFullName(command.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(command.rawPassword()));
        user.setRole(command.role());
        user.setIsActive(Boolean.TRUE);

        User saved = userGateway.save(user);
        auditUserCreated(saved, principal);

        return saved;
    }

    private void auditUserCreated(User user, AuthenticatedUser principal) {
        AuditEntry entry = principal == null
                ? new AuditEntry(null, null, null, AuditAction.USER_CREATED, "USER", user.getId(),
                        "perfil=" + user.getRole())
                : new AuditEntry(principal.id(), principal.email(), principal.role(), AuditAction.USER_CREATED,
                        "USER", user.getId(), "perfil=" + user.getRole());

        try {
            auditGateway.record(entry);
        } catch (RuntimeException ex) {
            log.warn("Falha ao registrar auditoria de cadastro: {}", ex.getMessage());
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
