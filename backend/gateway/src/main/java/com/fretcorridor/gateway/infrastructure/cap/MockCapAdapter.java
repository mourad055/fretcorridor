package com.fretcorridor.gateway.infrastructure.cap;

import com.fretcorridor.gateway.domain.cap.Capacite;
import com.fretcorridor.gateway.domain.cap.CapaciteEtat;
import com.fretcorridor.gateway.domain.cap.CapacitePort;
import com.fretcorridor.gateway.domain.cap.ModeCollecte;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * TODO(mobile): remplacer par l'appel réel à service-cap/service-mkt une fois ces
 * services livrés (Sprint 4 Mobile, @estie-glo). Deux transporteurs distincts sont
 * amorcés pour permettre la démonstration et le test de l'isolation par acteur.
 */
@Component
public class MockCapAdapter implements CapacitePort {

    private final List<Capacite> capacites = List.of(
            new Capacite("cap-1", "a0000000-0000-0000-0000-000000000002", "Camion 10T — LT 1234 AB",
                    "Douala", "Yaoundé", Instant.now().plus(1, ChronoUnit.DAYS),
                    9500, ModeCollecte.PORTE_A_PORTE, CapaciteEtat.PUBLIEE),
            new Capacite("cap-2", "a0000000-0000-0000-0000-000000000002", "Camion 10T — LT 1234 AB",
                    "Yaoundé", "Douala", Instant.now().plus(3, ChronoUnit.DAYS),
                    9500, ModeCollecte.POINT_DEPOT, CapaciteEtat.APPARIEE),
            new Capacite("cap-3", "a0000000-0000-0000-0000-000000000005", "Fourgon 3T — LT 5678 CD",
                    "Douala", "Bafoussam", Instant.now().plus(2, ChronoUnit.DAYS),
                    2800, ModeCollecte.PORTE_A_PORTE, CapaciteEtat.PUBLIEE)
    );

    @Override
    public Flux<Capacite> listerParTransporteur(String transporteurId) {
        return Flux.fromIterable(capacites).filter(c -> c.transporteurId().equals(transporteurId));
    }
}
