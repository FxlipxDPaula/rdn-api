package com.rdn.rdn_api.infrastructure.security;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.domain.UserRole;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Le o header {@code Authorization: Bearer <token>} e popula o contexto de
 * seguranca quando o token for valido.
 *
 * <p>Token ausente, expirado, mal assinado ou com role desconhecida nao
 * autenticam nada: a requisicao segue anonima e o {@code SecurityFilterChain}
 * responde {@code 401}. O filtro nunca responde por conta propria para manter
 * um unico formato de erro por status.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;

    public JwtAuthenticationFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            authenticate(header.substring(BEARER_PREFIX.length()).trim());
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        if (token.isEmpty()) {
            return;
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            UserRole role = firstRole(jwt.getClaimAsStringList("roles"));
            if (role == null) {
                return;
            }

            AuthenticatedUser principal = new AuthenticatedUser(
                    UUID.fromString(jwt.getSubject()),
                    jwt.getClaimAsString("name"),
                    jwt.getClaimAsString("email"),
                    role);

            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));

            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }
    }

    private static UserRole firstRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }

        try {
            return UserRole.valueOf(roles.get(0));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
