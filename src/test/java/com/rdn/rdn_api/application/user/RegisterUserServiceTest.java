package com.rdn.rdn_api.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.rdn.rdn_api.application.audit.AuditAction;
import com.rdn.rdn_api.application.audit.AuditEntry;
import com.rdn.rdn_api.application.audit.AuditGateway;
import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.application.user.exception.EmailAlreadyRegisteredException;
import com.rdn.rdn_api.domain.User;
import com.rdn.rdn_api.domain.UserRole;

/**
 * Testes unitarios do caso de uso de cadastro.
 *
 * <p>Usa BCrypt com forca 4 para manter o teste rapido; o algoritmo e o mesmo
 * do bean de producao.</p>
 */
class RegisterUserServiceTest {

    private static final String RAW_PASSWORD = "Senha@123";

    private static final String EMAIL = "novo@rdn.local";

    private final UserRegistrationGateway userGateway = mock(UserRegistrationGateway.class);

    private final AuditGateway auditGateway = mock(AuditGateway.class);

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private RegisterUserService service;

    private AuthenticatedUser admin;

    @BeforeEach
    void setUp() {
        service = new RegisterUserService(userGateway, passwordEncoder, auditGateway);
        admin = new AuthenticatedUser(UUID.randomUUID(), "Administrador", "admin@rdn.local", UserRole.ADMIN);
    }

    @Test
    void cadastraUsuarioComEmailNormalizadoESenhaHasheada() {
        when(userGateway.existsByEmail(EMAIL)).thenReturn(false);
        when(userGateway.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        User saved = service.register(
                new RegisterUserCommand("  Maria Silva  ", "  NOVO@RDN.local  ", RAW_PASSWORD, UserRole.CUSTOMER),
                admin);

        assertThat(saved.getFullName()).isEqualTo("Maria Silva");
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(saved.getIsActive()).isTrue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, saved.getPasswordHash())).isTrue();

        ArgumentCaptor<AuditEntry> audit = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditGateway).record(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo(AuditAction.USER_CREATED);
        assertThat(audit.getValue().actorEmail()).isEqualTo(admin.email());
        assertThat(audit.getValue().resource()).isEqualTo("USER");
    }

    @Test
    void recusaEmailJaCadastradoSemPersistir() {
        when(userGateway.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterUserCommand("Maria Silva", EMAIL, RAW_PASSWORD, UserRole.CUSTOMER), admin))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userGateway, never()).save(any(User.class));
        verifyNoInteractions(auditGateway);
    }
}
