package com.fretcorridor.gateway.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * RBAC de base (Sprint 1) : le rôle est évalué exclusivement depuis le JWT (RG-002).
 * Toute route hors périmètre d'un rôle répond 403 en JSON (jamais une redirection HTML
 * ni une erreur technique) — la garde de routes côté Angular s'appuie sur le même
 * découpage de chemins pour un comportement cohérent.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            JwtServerAuthenticationConverter converter,
            JwtReactiveAuthenticationManager authenticationManager
    ) {
        AuthenticationWebFilter jwtFilter = new AuthenticationWebFilter(authenticationManager);
        jwtFilter.setServerAuthenticationConverter(converter);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(spec -> spec
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/v1/bureau/**").hasRole("BUREAU")
                        .pathMatchers("/api/v1/transporteur/**").hasRole("TRANSPORTEUR")
                        .anyExchange().authenticated())
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Idempotency-Key"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
