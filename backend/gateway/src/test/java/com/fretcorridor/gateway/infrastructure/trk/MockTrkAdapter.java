package com.fretcorridor.gateway.infrastructure.trk;

import com.fretcorridor.gateway.domain.trk.PositionVehicule;
import com.fretcorridor.gateway.domain.trk.TrkPort;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Fixture de test uniquement — vit dans src/test/java, jamais sur le
 * classpath de production (même mécanisme que
 * ServiceIdaAuthenticationAdapter/MockIdaAuthenticationAdapter et
 * RealGeoAdapter/MockGeoAdapter, cf. docs/ROADMAP_INTEGRATION_gateway.md).
 * @Primary suffit à lever l'ambiguïté avec ServiceBurPositionAdapter
 * pendant les tests, puisque cette classe n'existe tout simplement pas
 * dans le jar déployé. Âges volontairement variés (récent, ancien) pour
 * démontrer que l'âge est toujours restitué, jamais une position figée
 * sans horodatage exploitable (RG-043).
 */
@Component
@Primary
public class MockTrkAdapter implements TrkPort {

    private final List<PositionVehicule> positions = List.of(
            new PositionVehicule("pos-1", "tenant-bgft-douala", "Camion 10T — LT 1234 AB",
                    4.0511, 9.7679, Instant.now().minus(90, ChronoUnit.SECONDS)),
            new PositionVehicule("pos-2", "tenant-bgft-douala", "Fourgon 3T — LT 5678 CD",
                    4.6167, 11.5167, Instant.now().minus(25, ChronoUnit.MINUTES)),
            new PositionVehicule("pos-3", "tenant-bnft-ndjamena", "Camion 8T — TD 4321 EF",
                    12.1348, 15.0557, Instant.now().minus(5, ChronoUnit.MINUTES)),
            new PositionVehicule("pos-4", "tenant-bnft-ndjamena", "Camion 10T — TD 9012 GH",
                    9.15, 18.3833, Instant.now().minus(40, ChronoUnit.MINUTES))
    );

    @Override
    public Flux<PositionVehicule> listerPositionsParTenant(String tenantId) {
        return Flux.fromIterable(positions).filter(p -> p.tenantId().equals(tenantId));
    }
}
