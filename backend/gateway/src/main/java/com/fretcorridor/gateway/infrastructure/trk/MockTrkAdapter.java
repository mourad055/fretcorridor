package com.fretcorridor.gateway.infrastructure.trk;

import com.fretcorridor.gateway.domain.trk.PositionVehicule;
import com.fretcorridor.gateway.domain.trk.TrkPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * TODO(moteur): remplacer par l'appel réel à service-trk une fois ce service
 * livré (Sprint 6 Moteur, @stevetelecom, issue #21). Âges volontairement
 * variés (récent, ancien) pour démontrer que l'âge est toujours restitué,
 * jamais une position figée sans horodatage exploitable (RG-043).
 */
@Component
public class MockTrkAdapter implements TrkPort {

    private final List<PositionVehicule> positions = List.of(
            new PositionVehicule("pos-1", "tenant-bgft-douala", "Camion 10T — LT 1234 AB",
                    4.0511, 9.7679, Instant.now().minus(90, ChronoUnit.SECONDS)),
            new PositionVehicule("pos-2", "tenant-bgft-douala", "Fourgon 3T — LT 5678 CD",
                    4.6167, 11.5167, Instant.now().minus(25, ChronoUnit.MINUTES)),
            new PositionVehicule("pos-3", "tenant-bgft-tchad", "Camion 8T — TD 4321 EF",
                    12.1348, 15.0557, Instant.now().minus(5, ChronoUnit.MINUTES))
    );

    @Override
    public Flux<PositionVehicule> listerPositionsParTenant(String tenantId) {
        return Flux.fromIterable(positions).filter(p -> p.tenantId().equals(tenantId));
    }
}
