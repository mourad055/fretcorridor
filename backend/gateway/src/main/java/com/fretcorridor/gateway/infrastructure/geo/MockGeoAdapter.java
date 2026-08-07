package com.fretcorridor.gateway.infrastructure.geo;

import com.fretcorridor.gateway.domain.geo.Axe;
import com.fretcorridor.gateway.domain.geo.AxeEtatActivation;
import com.fretcorridor.gateway.domain.geo.GeoPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * TODO(moteur): remplacer par l'appel réel à service-geo (référentiel Axe/Hub,
 * zonage H3) une fois ce service livré (Sprint 3 Moteur, @stevetelecom, issue #21).
 * Deux tenants distincts sont amorcés pour permettre la démonstration et le test
 * de l'isolation multi-tenant (ENF-MUL-01).
 */
@Component
public class MockGeoAdapter implements GeoPort {

    private final List<Axe> axes = List.of(
            new Axe("axe-1", "tenant-bgft-douala", "Douala", "Yaoundé", 300, AxeEtatActivation.PAIEMENT),
            new Axe("axe-2", "tenant-bgft-douala", "Douala", "Bafoussam", 350, AxeEtatActivation.MATCHING),
            new Axe("axe-3", "tenant-bnft-ndjamena", "N'Djamena", "Moundou", 470, AxeEtatActivation.VISIBILITE),
            new Axe("axe-4", "tenant-bnft-ndjamena", "N'Djamena", "Sarh", 550, AxeEtatActivation.MATCHING)
    );

    @Override
    public Flux<Axe> listerAxesParTenant(String tenantId) {
        return Flux.fromIterable(axes).filter(axe -> axe.tenantId().equals(tenantId));
    }
}
