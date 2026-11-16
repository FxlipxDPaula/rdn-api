package com.rdn.rdn_api.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Configuracao da especificacao OpenAPI exposta pelo springdoc.
 *
 * <p>A seguranca e declarada como esquema bearer e aplicada por anotacao nos
 * controllers protegidos; assim o login continua documentado como publico.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI rdnOpenApi() {
        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Token obtido em POST /auth/login");

        return new OpenAPI()
                .info(new Info()
                        .title("RDN API")
                        .version("1.0.0")
                        .description("API da rede de lanchonetes: pedidos multicanal, pagamento mock, "
                                + "fidelidade com consentimento, promocoes, catalogo e auditoria."))
                .addServersItem(new Server().url("http://localhost:8080").description("Ambiente local"))
                .components(new Components().addSecuritySchemes("bearerAuth", bearerAuth));
    }
}
