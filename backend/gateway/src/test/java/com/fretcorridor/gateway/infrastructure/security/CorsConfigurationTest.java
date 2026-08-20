package com.fretcorridor.gateway.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Régression : un navigateur envoie l'en-tête Origin sur les requêtes
 * POST/PUT/DELETE même quand elles sont same-origin côté navigateur (ex. via
 * le proxy Netlify). Sans le front déployé dans la liste blanche, Spring
 * Security rejette la requête en 403 avant d'atteindre le contrôleur —
 * observé en prod sur /api/v1/auth/login (cf. docs déploiement Sprint D).
 *
 * Unit test direct sur la CorsConfigurationSource plutôt qu'un aller-retour
 * WebTestClient complet : ce dernier ne reproduit pas fidèlement le
 * traitement CORS réel sur cette version de Spring (vérifié manuellement —
 * une requête réelle avec le même en-tête Origin passe correctement).
 */
class CorsConfigurationTest {

    private final CorsConfigurationSource source = new SecurityConfig().corsConfigurationSource();

    private String allowedOriginFor(String origin) {
        var request = MockServerHttpRequest.post("/api/v1/auth/login")
                .header("Origin", origin)
                .build();
        CorsConfiguration config = source.getCorsConfiguration(MockServerWebExchange.from(request));
        return config == null ? null : config.checkOrigin(origin);
    }

    @Test
    void allows_the_deployed_netlify_origin() {
        assertThat(allowedOriginFor("https://fretcorridor-web.netlify.app"))
                .isEqualTo("https://fretcorridor-web.netlify.app");
    }

    @Test
    void allows_localhost_dev_origins() {
        assertThat(allowedOriginFor("http://localhost:4200")).isEqualTo("http://localhost:4200");
        assertThat(allowedOriginFor("http://localhost:4201")).isEqualTo("http://localhost:4201");
    }

    @Test
    void rejects_an_unknown_origin() {
        assertThat(allowedOriginFor("https://evil.example.com")).isNull();
    }
}
