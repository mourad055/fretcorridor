package com.fretcorridor.gateway.infrastructure.geo;

import com.fretcorridor.gateway.domain.geo.Axe;
import com.fretcorridor.gateway.domain.geo.GeoPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * DEPRECIE - remplace par RealGeoAdapter (service-geo reellement branche,
 * Sprint 3 Moteur livre). Conserve temporairement pour reference/rollback,
 * a supprimer une fois RealGeoAdapter valide en integration.
 */
@Component
@org.springframework.context.annotation.Profile("mock-geo")
public class MockGeoAdapter implements GeoPort {

    private final List<Axe> axes = List.of(
            new Axe("axe-1", "tenant-bgft-douala", "Douala", "Yaoundé", 300, true, true, true),
            new Axe("axe-2", "tenant-bgft-douala", "Douala", "Bafoussam", 350, true, true, false),
            new Axe("axe-3", "tenant-bnft-ndjamena", "N'Djamena", "Moundou", 470, true, false, false),
            new Axe("axe-4", "tenant-bnft-ndjamena", "N'Djamena", "Sarh", 550, true, true, false)
    );

    @Override
    public Flux<Axe> listerAxesParTenant(String tenantId) {
        return Flux.fromIterable(axes).filter(axe -> axe.tenantId().equals(tenantId));
    }
}
