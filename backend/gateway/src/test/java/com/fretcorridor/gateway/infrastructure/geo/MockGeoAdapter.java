package com.fretcorridor.gateway.infrastructure.geo;

import com.fretcorridor.gateway.domain.geo.Axe;
import com.fretcorridor.gateway.domain.geo.GeoPort;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Fixture de test uniquement — vit dans src/test/java, jamais sur le
 * classpath de production (même mécanisme que
 * ServiceIdaAuthenticationAdapter/MockIdaAuthenticationAdapter, cf.
 * docs/ROADMAP_INTEGRATION_gateway.md). @Primary suffit à lever
 * l'ambiguïté avec RealGeoAdapter pendant les tests, puisque cette classe
 * n'existe tout simplement pas dans le jar déployé.
 *
 * Garde 2 tenants de démonstration (contrairement à service-geo réel, qui
 * n'en porte qu'un en Phase 1 — cf. docs/adr, décision mono-tenant GEO) :
 * ce test vérifie le CONTRAT du port GeoPort (« ne jamais restituer les
 * axes d'un autre tenant »), pas le comportement de RealGeoAdapter, qui ne
 * le tient pas lui-même en Phase 1 (cf. AxeControllerIsolationTest).
 */
@Component
@Primary
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
