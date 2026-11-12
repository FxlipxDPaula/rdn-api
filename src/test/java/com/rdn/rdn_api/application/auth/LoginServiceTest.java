package com.rdn.rdn_api.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.rdn.rdn_api.domain.User;
import com.rdn.rdn_api.domain.UserRole;

/**
 * Testes unitarios do caso de uso de login.
 *
 * <p>Usa {@code BCryptPasswordEncoder} com forca 4 (em vez de 10) apenas para
 * manter o teste rapido: o algoritmo e o mesmo, muda so o custo.</p>
 */
class LoginServiceTest {

    private static final String EMAIL = "cliente@rdn.local";

    private static final String RAW_PASSWORD = "Cliente@123";

    private final AuthUserGateway userGateway = mock(AuthUserGateway.class);

    private final TokenIssuer tokenIssuer = mock(TokenIssuer.class);

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginService(userGateway, passwordEncoder, tokenIssuer);
    }

    @Test
    void autenticaUsuarioAtivoComCredenciaisValidas() {
        User user = activeUser();
        when(userGateway.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tokenIssuer.issue(user)).thenReturn(new TokenIssuer.IssuedToken("token-jwt", 3600));

        LoginResult result = loginService.login(new LoginCommand("  CLIENTE@rdn.local  ", RAW_PASSWORD));

        assertThat(result.accessToken()).isEqualTo("token-jwt");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresInSeconds()).isEqualTo(3600);
        assertThat(result.user().email()).isEqualTo(EMAIL);
        assertThat(result.user().role()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void recusaSenhaIncorreta() {
        when(userGateway.findByEmail(EMAIL)).thenReturn(Optional.of(activeUser()));

        assertThatThrownBy(() -> loginService.login(new LoginCommand(EMAIL, "senha-errada")))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void recusaEmailInexistenteSemEmitirToken() {
        when(userGateway.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.login(new LoginCommand(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void recusaSenhaNulaSemConsultarHash() {
        when(userGateway.findByEmail(EMAIL)).thenReturn(Optional.of(activeUser()));

        assertThatThrownBy(() -> loginService.login(new LoginCommand(EMAIL, null)))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void recusaUsuarioInativoAposCredenciaisCorretas() {
        User inactive = activeUser();
        inactive.deactivate();
        when(userGateway.findByEmail(EMAIL)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> loginService.login(new LoginCommand(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(InactiveUserException.class);

        verifyNoInteractions(tokenIssuer);
    }

    private User activeUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName("Cliente Teste");
        user.setEmail(EMAIL);
        user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
        user.setRole(UserRole.CUSTOMER);
        user.setIsActive(Boolean.TRUE);
        return user;
    }
}
