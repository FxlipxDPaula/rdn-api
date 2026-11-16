package com.rdn.rdn_api.infrastructure.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * Configuracao de seguranca da API.
 *
 * <p>Modelo stateless: nenhuma sessao HTTP, todo acesso vem de JWT. Somente
 * {@code /auth/login} e o endpoint interno {@code /error} sao publicos; o
 * restante exige token valido. O filtro JWT e instanciado aqui, e nao como
 * bean, para o Spring Boot nao registra-lo duas vezes no container de
 * servlets.</p>
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {

        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtDecoder);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // pedidos: cliente cria e consulta; operacao lista e move status
                        .requestMatchers(HttpMethod.POST, "/pedidos").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/pedidos").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.PATCH, "/pedidos/*/status").hasAnyRole("KITCHEN", "STAFF", "ADMIN")
                        // pagamento: mesmo conjunto de quem pode criar o pedido
                        .requestMatchers(HttpMethod.POST, "/pedidos/*/pagamentos").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                        // fidelidade: apenas o cliente dono da conta
                        .requestMatchers(HttpMethod.GET, "/fidelidade", "/fidelidade/transacoes").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PUT, "/fidelidade/consentimento").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/fidelidade/resgates").hasRole("CUSTOMER")
                        // consultas de catalogo e campanhas: qualquer autenticado
                        .requestMatchers(HttpMethod.GET, "/promocoes").authenticated()
                        .requestMatchers(HttpMethod.GET, "/unidades", "/unidades/*/cardapio").authenticated()
                        // cadastro de usuarios: somente administracao
                        .requestMatchers(HttpMethod.POST, "/usuarios").hasRole("ADMIN")
                        // auditoria: somente administracao
                        .requestMatchers(HttpMethod.GET, "/auditoria").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(hmacKey(jwtProperties)));
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
        return NimbusJwtDecoder.withSecretKey(hmacKey(jwtProperties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private static SecretKey hmacKey(JwtProperties jwtProperties) {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "security.jwt.secret deve ter pelo menos " + MIN_SECRET_BYTES + " bytes (HS256)");
        }

        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
