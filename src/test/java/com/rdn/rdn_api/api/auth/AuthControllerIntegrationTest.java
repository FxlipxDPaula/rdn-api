package com.rdn.rdn_api.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.rdn.rdn_api.domain.UserRole;
import com.rdn.rdn_api.infrastructure.persistence.entity.UserJpaEntity;
import com.rdn.rdn_api.infrastructure.repository.UserJpaRepository;

/**
 * Teste ponta a ponta dos endpoints de autenticacao sobre HTTP real.
 *
 * <p>Sobe a aplicacao em porta aleatoria para exercitar toda a cadeia:
 * Spring MVC, filtro JWT, entry point e handlers de erro. Usa um banco H2
 * proprio para nao interferir nos testes de persistencia.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class AuthControllerIntegrationTest {

    private static final String EMAIL = "cliente.teste@rdn.local";

    private static final String PASSWORD = "Cliente@123";

    private static final String INACTIVE_EMAIL = "inativo.teste@rdn.local";

    private static final Pattern ACCESS_TOKEN = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private Environment environment;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        persistUser(EMAIL, true);
        persistUser(INACTIVE_EMAIL, false);
    }

    @Test
    void loginValidoRetornaTokenBearer() throws Exception {
        HttpResponse<String> response = postLogin(credentials(EMAIL, PASSWORD));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"tokenType\":\"Bearer\"")
                .contains("\"expiresIn\":3600")
                .contains("\"email\":\"" + EMAIL + "\"")
                .contains("\"role\":\"CUSTOMER\"")
                .doesNotContain("passwordHash");

        assertThat(extractToken(response.body())).isNotBlank();
    }

    @Test
    void loginComSenhaInvalidaRetorna401() throws Exception {
        HttpResponse<String> response = postLogin(credentials(EMAIL, "senha-errada"));

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"error\":\"CREDENCIAIS_INVALIDAS\"");
    }

    @Test
    void loginComEmailInexistenteRetorna401() throws Exception {
        HttpResponse<String> response = postLogin(credentials("ninguem@rdn.local", PASSWORD));

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"error\":\"CREDENCIAIS_INVALIDAS\"");
    }

    @Test
    void loginComCamposVaziosRetorna400ComDetalhes() throws Exception {
        HttpResponse<String> response = postLogin("{\"email\":\"\",\"senha\":\"\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body())
                .contains("\"error\":\"DADOS_INVALIDOS\"")
                .contains("\"field\":\"email\"")
                .contains("\"field\":\"senha\"");
    }

    @Test
    void loginDeUsuarioInativoRetorna403() throws Exception {
        HttpResponse<String> response = postLogin(credentials(INACTIVE_EMAIL, PASSWORD));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"error\":\"USUARIO_INATIVO\"");
    }

    @Test
    void meSemTokenRetorna401() throws Exception {
        HttpResponse<String> response = getMe(null);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"error\":\"NAO_AUTENTICADO\"");
    }

    @Test
    void meComTokenValidoRetornaUsuario() throws Exception {
        String token = extractToken(postLogin(credentials(EMAIL, PASSWORD)).body());

        HttpResponse<String> response = getMe(token);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"email\":\"" + EMAIL + "\"")
                .contains("\"role\":\"CUSTOMER\"");
    }

    @Test
    void meComTokenAdulteradoRetorna401() throws Exception {
        String token = extractToken(postLogin(credentials(EMAIL, PASSWORD)).body());

        HttpResponse<String> response = getMe(token + "x");

        assertThat(response.statusCode()).isEqualTo(401);
    }

    private void persistUser(String email, boolean active) {
        userRepository.findByEmail(email).ifPresent(userRepository::delete);

        UserJpaEntity entity = new UserJpaEntity();
        entity.setFullName("Usuario de Teste");
        entity.setEmail(email);
        entity.setPasswordHash(passwordEncoder.encode(PASSWORD));
        entity.setRole(UserRole.CUSTOMER);
        entity.setIsActive(active);
        userRepository.saveAndFlush(entity);
    }

    private HttpResponse<String> postLogin(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getMe(String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl() + "/auth/me")).GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String baseUrl() {
        return "http://localhost:" + environment.getProperty("local.server.port");
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, password);
    }

    private static String extractToken(String body) {
        Matcher matcher = ACCESS_TOKEN.matcher(body);
        return matcher.find() ? matcher.group(1) : "";
    }
}
