package com.rdn.rdn_api.api.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/**
 * Verifica que a documentacao OpenAPI e a UI do Swagger sobem sem token.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:docs_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class OpenApiDocsTest {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Autowired
    private Environment environment;

    @Test
    void expoeApiDocsESwaggerSemAutenticacao() throws Exception {
        HttpResponse<String> apiDocs = get("/v3/api-docs");
        assertThat(apiDocs.statusCode()).isEqualTo(200);
        assertThat(apiDocs.body())
                .contains("/auth/login")
                .contains("/pedidos")
                .contains("/fidelidade")
                .contains("/auditoria")
                .contains("bearerAuth");

        HttpResponse<String> swagger = get("/swagger-ui.html");
        assertThat(swagger.statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + environment.getProperty("local.server.port") + path))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
