package com.fretcorridor.gateway.infrastructure.opt;

import com.fretcorridor.gateway.domain.opt.MissionAppariee;
import com.fretcorridor.gateway.domain.opt.OptPort;
import com.fretcorridor.gateway.domain.opt.StatutMission;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Fixture de test uniquement — vit dans src/test/java, jamais sur le
 * classpath de production (même mécanisme que RealGeoAdapter/MockGeoAdapter
 * et ServiceIdaAuthenticationAdapter/MockIdaAuthenticationAdapter, cf.
 * docs/ROADMAP_INTEGRATION_gateway.md). Remplacée en production par
 * ServiceBurMissionAppparieeAdapter, qui ne connaît qu'un seul statut
 * (CONFIRMEE) — ce mock garde les 3 statuts et 2 tenants historiques pour
 * que les suites de test existantes (filtre par statut, isolation) n'aient
 * rien à changer.
 */
@Component
@Primary
public class MockOptAdapter implements OptPort {

    private final List<MissionAppariee> missions = List.of(
            new MissionAppariee("mission-1", "tenant-bgft-douala", "axe-1", "Transport Étoile SARL",
                    "Douala", "Yaoundé", Instant.now().plus(1, ChronoUnit.DAYS), StatutMission.CONFIRMEE),
            new MissionAppariee("mission-2", "tenant-bgft-douala", "axe-2", "Jean Mbarga",
                    "Douala", "Bafoussam", Instant.now().minus(2, ChronoUnit.HOURS), StatutMission.EN_COURS),
            new MissionAppariee("mission-3", "tenant-bnft-ndjamena", "axe-3", "Transporteur Sahel",
                    "N'Djamena", "Moundou", Instant.now().minus(1, ChronoUnit.DAYS), StatutMission.CLOTUREE),
            new MissionAppariee("mission-4", "tenant-bnft-ndjamena", "axe-4", "Logistique Sahel Tchad",
                    "N'Djamena", "Sarh", Instant.now().plus(6, ChronoUnit.HOURS), StatutMission.EN_COURS)
    );

    @Override
    public Flux<MissionAppariee> listerMissionsParTenant(String tenantId) {
        return Flux.fromIterable(missions).filter(m -> m.tenantId().equals(tenantId));
    }
}
