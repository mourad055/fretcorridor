package com.fretcorridor.gateway.infrastructure.security;

import com.fretcorridor.gateway.domain.Actor;
import com.fretcorridor.gateway.domain.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("dev-secret-change-me-in-production-min-32-bytes", 60);

    @Test
    void issues_a_token_carrying_role_and_tenant_claims() {
        Actor actor = new Actor("actor-1", "+237600000001", Role.BUREAU, "tenant-bgft-douala", "ida-token-abc");

        String token = jwtService.issue(actor);
        Optional<Claims> claims = jwtService.parse(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("actor-1");
        assertThat(claims.get().get("role", String.class)).isEqualTo("BUREAU");
        assertThat(claims.get().get("tenantId", String.class)).isEqualTo("tenant-bgft-douala");
        assertThat(JwtService.roleOf(claims.get())).isEqualTo(Role.BUREAU);
    }

    @Test
    void round_trips_the_service_ida_delegation_token_so_the_gateway_can_call_mobile_services_on_the_actors_behalf() {
        Actor actor = new Actor("actor-1", "+237600000001", Role.BUREAU, "tenant-bgft-douala", "ida-token-abc");

        String token = jwtService.issue(actor);
        Claims claims = jwtService.parse(token).orElseThrow();

        assertThat(JwtService.delegationTokenOf(claims)).isEqualTo("ida-token-abc");
    }

    @Test
    void omits_the_delegation_token_claim_when_the_actor_has_none() {
        Actor actor = new Actor("actor-1", "+237600000001", Role.BUREAU, "tenant-bgft-douala", null);

        String token = jwtService.issue(actor);
        Claims claims = jwtService.parse(token).orElseThrow();

        assertThat(JwtService.delegationTokenOf(claims)).isNull();
    }

    @Test
    void rejects_a_token_signed_with_a_different_secret() {
        JwtService other = new JwtService("another-secret-entirely-different-32b", 60);
        Actor actor = new Actor("actor-2", "+237600000002", Role.ADMIN, "tenant-flysoft", "ida-token-def");

        String token = other.issue(actor);

        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    void rejects_a_malformed_token() {
        assertThat(jwtService.parse("not-a-jwt")).isEmpty();
    }
}
