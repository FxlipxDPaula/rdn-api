package com.rdn.rdn_api.api.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
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
 * Teste ponta a ponta do cadastro de usuarios sobre HTTP real.
 *
 * <p>Exercita o filtro JWT, a regra ADMIN do SecurityConfig, o BCrypt e o
 * handler de e-mail duplicado, usando um banco H2 proprio.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:user_registration_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class UserRegistrationIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@rdn.local";

    private static final String ADMIN_PASSWORD = "Admin@123";

    private static final String CUSTOMER_EMAIL = "customer@rdn.local";

    private static final String CUSTOMER_PASSWORD = "Customer@123";

    private static final String NEW_PASSWORD = "Senha@123";

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
        persistUser(ADMIN_EMAIL, ADMIN_PASSWORD, UserRole.ADMIN);
        persistUser(CUSTOMER_EMAIL, CUSTOMER_PASSWORD, UserRole.CUSTOMER);
    }

    @Test
    void adminCadastraUsuarioELoginFunciona() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String email = uniqueEmail();

        HttpResponse<String> created = postUsuario(adminToken,
                registerBody("Maria Silva", email, NEW_PASSWORD, "CUSTOMER"));

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body())
                .contains("\"email\":\"" + email + "\"")
                .contains("\"perfil\":\"CUSTOMER\"")
                .contains("\"ativo\":true")
                .doesNotContain("passwordHash");

        assertThat(login(email, NEW_PASSWORD)).isNotBlank();
    }

    @Test
    void emailDuplicadoRetorna409() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String email = uniqueEmail();
        String body = registerBody("Maria Silva", email, NEW_PASSWORD, "CUSTOMER");

        assertThat(postUsuario(adminToken, body).statusCode()).isEqualTo(201);

        HttpResponse<String> duplicate = postUsuario(adminToken, body);

        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(duplicate.body()).contains("\"error\":\"EMAIL_JA_CADASTRADO\"");
    }

    @Test
    void clienteNaoPodeCadastrarUsuario() throws Exception {
        String customerToken = login(CUSTOMER_EMAIL, CUSTOMER_PASSWORD);

        HttpResponse<String> response = postUsuario(customerToken,
                registerBody("Maria Silva", uniqueEmail(), NEW_PASSWORD, "CUSTOMER"));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"error\":\"ACESSO_NEGADO\"");
    }

    @Test
    void semTokenRetorna401() throws Exception {
        HttpResponse<String> response = postUsuario(null,
                registerBody("Maria Silva", uniqueEmail(), NEW_PASSWORD, "CUSTOMER"));

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"error\":\"NAO_AUTENTICADO\"");
    }

    @Test
    void camposInvalidosRetornam400ComDetalhes() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        HttpResponse<String> response = postUsuario(adminToken,
                "{\"nome\":\"\",\"email\":\"invalido\",\"senha\":\"123\",\"perfil\":null}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body())
                .contains("\"error\":\"DADOS_INVALIDOS\"")
                .contains("\"field\":\"nome\"")
                .contains("\"field\":\"email\"")
                .contains("\"field\":\"senha\"")
                .contains("\"field\":\"perfil\"");
    }

    private void persistUser(String email, String password, UserRole role) {
        userRepository.findByEmail(email).ifPresent(userRepository::delete);

        UserJpaEntity entity = new UserJpaEntity();
        entity.setFullName("Usuario de Teste");
        entity.setEmail(email);
        entity.setPasswordHash(passwordEncoder.encode(password));
        entity.setRole(role);
        entity.setIsActive(Boolean.TRUE);
        userRepository.saveAndFlush(entity);
    }

    private String login(String email, String password) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, password)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        return extractToken(response.body());
    }

    private HttpResponse<String> postUsuario(String token, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl() + "/usuarios"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String baseUrl() {
        return "http://localhost:" + environment.getProperty("local.server.port");
    }

    private static String registerBody(String name, String email, String password, String role) {
        return "{\"nome\":\"%s\",\"email\":\"%s\",\"senha\":\"%s\",\"perfil\":\"%s\"}"
                .formatted(name, email, password, role);
    }

    private static String uniqueEmail() {
        return "novo." + UUID.randomUUID() + "@rdn.local";
    }

    private static String extractToken(String body) {
        Matcher matcher = ACCESS_TOKEN.matcher(body);
        return matcher.find() ? matcher.group(1) : "";
    }
}
