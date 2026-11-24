package com.rdn.rdn_api.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rdn.rdn_api.api.error.ApiError;
import com.rdn.rdn_api.api.error.ApiErrorDetail;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Configuracao da especificacao OpenAPI exposta pelo springdoc.
 *
 * <p>A seguranca e declarada como esquema bearer e aplicada por anotacao nos
 * controllers protegidos; assim o login continua documentado como publico.</p>
 *
 * <p>O servidor declarado na especificacao e relativo ("/") por padrao: o botao
 * Try it out chama a mesma origem que abriu o Swagger UI, funcionando tanto em
 * http://localhost:8080 quanto em uma URL publica de deploy. A propriedade
 * app.public-url permite fixar uma URL absoluta quando necessario.</p>
 *
 * <p>Toda resposta de erro documentada com {@code @ApiResponse} recebe
 * automaticamente o schema {@link ApiError}, mantendo um unico contrato de falha
 * em todos os endpoints sem repetir {@code content} em cada anotacao.</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String API_ERROR_SCHEMA = "ApiError";

    private static final String API_ERROR_REF = "#/components/schemas/" + API_ERROR_SCHEMA;

    private static final String JSON = "application/json";

    private final String publicUrl;

    public OpenApiConfig(@Value("${app.public-url:}") String publicUrl) {
        this.publicUrl = publicUrl;
    }

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
                .addServersItem(server())
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerAuth)
                        .schemas(errorSchemas()));
    }

    /**
     * Aplica o schema de erro a toda resposta nao-2xx que ainda nao declare corpo.
     *
     * @return customizer executado pelo springdoc ao montar cada operacao
     */
    @Bean
    OperationCustomizer apiErrorResponses() {
        return (operation, handlerMethod) -> {
            Map<String, ApiResponse> responses = operation.getResponses();
            if (responses == null) {
                return operation;
            }

            responses.forEach((statusCode, response) -> {
                boolean success = statusCode.startsWith("2");
                if (success || response.getContent() != null) {
                    return;
                }
                response.setContent(new Content().addMediaType(JSON,
                        new MediaType().schema(new Schema<>().$ref(API_ERROR_REF))));
            });

            return operation;
        };
    }

    private Map<String, Schema> errorSchemas() {
        Map<String, Schema> schemas = new LinkedHashMap<>();
        schemas.put(API_ERROR_SCHEMA, resolve(ApiError.class));
        schemas.put("ApiErrorDetail", resolve(ApiErrorDetail.class));
        return schemas;
    }

    private Schema<?> resolve(Class<?> type) {
        ResolvedSchema resolved = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(type));
        return resolved.schema;
    }

    private Server server() {
        if (publicUrl == null || publicUrl.isBlank()) {
            return new Server().url("/")
                    .description("Mesma origem do Swagger UI (localhost ou deploy)");
        }
        return new Server().url(stripTrailingSlash(publicUrl.trim()))
                .description("URL publica configurada em APP_PUBLIC_URL");
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
