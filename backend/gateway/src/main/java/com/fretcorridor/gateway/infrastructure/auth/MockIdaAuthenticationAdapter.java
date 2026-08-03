package com.fretcorridor.gateway.infrastructure.auth;

import com.fretcorridor.gateway.domain.Actor;
import com.fretcorridor.gateway.domain.AuthenticationPort;
import com.fretcorridor.gateway.domain.InvalidCredentialsException;
import com.fretcorridor.gateway.domain.Role;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * TODO(mobile): remplacer par l'appel réel à service-ida (KYC gradué, niveau 2 requis)
 * une fois ce service livré (Sprint 1-2, Personne 1). Adaptateur mock documenté,
 * conforme à la règle "scope strict" du PRD §10.4 : ne jamais implémenter le service
 * manquant à sa place, seulement un mock explicite.
 *
 * Code à usage unique fixe "123456" en environnement de développement uniquement.
 */
@Component
public class MockIdaAuthenticationAdapter implements AuthenticationPort {

    private static final String DEV_CODE = "123456";

    private final Map<String, Actor> actorsByPhone = Map.of(
            "+237600000001", new Actor("actor-bureau-1", "+237600000001", Role.BUREAU, "tenant-bgft-douala"),
            "+237600000002", new Actor("actor-transporteur-1", "+237600000002", Role.TRANSPORTEUR, "tenant-bgft-douala"),
            "+237600000003", new Actor("actor-admin-1", "+237600000003", Role.ADMIN, "tenant-flysoft")
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
