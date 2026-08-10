package com.fretcorridor.gateway.infrastructure.security;

import com.fretcorridor.gateway.domain.Actor;
import com.fretcorridor.gateway.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import reactor.test.StepVerifier;

/**
 * Ferme la boucle du token de délégation service-ida (double autorité JWT,
 * cf. docs/ROADMAP_INTEGRATION_gateway.md) : prouve que ce que
 * ServiceIdaAuthenticationAdapter capture ressort intact dans le principal
 * exploitable par les contrôleurs, pas seulement dans le JWT lui-même
 * (déjà couvert par JwtServiceTest).
 */
class JwtReactiveAuthenticationManagerTest {

    private final JwtService jwtService = new JwtService("dev-secret-change-me-in-production-min-32-bytes", 60);
    private final JwtReactiveAuthenticationManager manager = new JwtReactiveAuthenticationManager(jwtService);

    @Test
    void resolves_a_principal_carrying_the_service_ida_delegation_token() {
        Actor actor = new Actor("actor-1", "+237600000001", Role.BUREAU, "tenant-bgft-douala", "ida-token-abc");
        String token = jwtService.issue(actor);
        Authentication requete = new UsernamePasswordAuthenticationToken(null, token);

        StepVerifier.create(manager.authenticate(requete))
                .assertNext(authentification -> {
                    AuthenticatedActor principal = (AuthenticatedActor) authentification.getPrincipal();
                    org.assertj.core.api.Assertions.assertThat(principal.delegationToken()).isEqualTo("ida-token-abc");
                })
                .verifyComplete();
    }

    @Test
    void resolves_a_principal_with_a_null_delegation_token_when_the_actor_had_none() {
        Actor actor = new Actor("actor-1", "+237600000001", Role.BUREAU, "tenant-bgft-douala", null);
        String token = jwtService.issue(actor);
        Authentication requete = new UsernamePasswordAuthenticationToken(null, token);

        StepVerifier.create(manager.authenticate(requete))
                .assertNext(authentification -> {
                    AuthenticatedActor principal = (AuthenticatedActor) authentification.getPrincipal();
                    org.assertj.core.api.Assertions.assertThat(principal.delegationToken()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void rejects_an_invalid_token() {
        Authentication requete = new UsernamePasswordAuthenticationToken(null, "not-a-jwt");

        StepVerifier.create(manager.authenticate(requete))
                .expectError(BadCredentialsException.class)
                .verify();
    }
}
