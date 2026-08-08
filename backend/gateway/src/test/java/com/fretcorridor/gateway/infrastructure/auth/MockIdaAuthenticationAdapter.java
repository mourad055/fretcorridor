package com.fretcorridor.gateway.infrastructure.auth;

import com.fretcorridor.gateway.domain.Actor;
import com.fretcorridor.gateway.domain.AuthenticationPort;
import com.fretcorridor.gateway.domain.InvalidCredentialsException;
import com.fretcorridor.gateway.domain.Role;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Fixture de test uniquement — vit dans src/test/java, jamais sur le
 * classpath de production (cf. docs/ROADMAP_INTEGRATION_gateway.md, Phase 1).
 * Remplace {@link ServiceIdaAuthenticationAdapter} pendant les tests
 * @SpringBootTest : @Primary suffit à lever l'ambiguïté puisque cette classe
 * n'existe tout simplement pas dans le jar déployé. Garde les 5 acteurs de
 * démonstration historiques avec le code fixe "123456", pour que les suites
 * d'isolation existantes n'aient rien à changer.
 */
@Component
@Primary
public class MockIdaAuthenticationAdapter implements AuthenticationPort {

    private static final String DEV_CODE = "123456";

    private final Map<String, Actor> actorsByPhone = Map.of(
            "+237600000001", new Actor("actor-bureau-1", "+237600000001", Role.BUREAU, "tenant-bgft-douala"),
            "+237600000002", new Actor("actor-transporteur-1", "+237600000002", Role.TRANSPORTEUR, "tenant-bgft-douala"),
            "+237600000003", new Actor("actor-admin-1", "+237600000003", Role.ADMIN, "tenant-flysoft"),
            // Second tenant Bureau, pour les tests et démonstrations d'isolation multi-tenant (ENF-MUL-01).
            "+235600000004", new Actor("actor-bureau-2", "+235600000004", Role.BUREAU, "tenant-bnft-ndjamena"),
            // Second Transporteur du même tenant, pour les tests d'isolation par acteur (PRD §5.3).
            "+237600000005", new Actor("actor-transporteur-2", "+237600000005", Role.TRANSPORTEUR, "tenant-bgft-douala")
    );

    @Override
    public Mono<Actor> authenticate(String phone, String code) {
        Actor actor = actorsByPhone.get(phone);
        if (actor == null || !DEV_CODE.equals(code)) {
            return Mono.error(new InvalidCredentialsException());
        }
        return Mono.just(actor);
    }
}
