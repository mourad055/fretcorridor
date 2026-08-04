package com.fretcorridor.gateway.infrastructure.opt;

import com.fretcorridor.gateway.domain.opt.MissionAppariee;
import com.fretcorridor.gateway.domain.opt.OptPort;
import com.fretcorridor.gateway.domain.opt.StatutMission;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * TODO(moteur): remplacer par l'appel réel à service-opt une fois ce service
 * livré (Sprint 5 Moteur, @stevetelecom, issue #21). Deux tenants distincts sont
 * amorcés pour rester cohérent avec l'isolation déjà prouvée sur les axes
 * (Sprint 3, ENF-MUL-01).
 */
@Component
public class MockOptAdapter implements OptPort {

    private final List<MissionAppariee> missions = List.of(
            new MissionAppariee("mission-1", "tenant-bgft-douala", "axe-1", "Transport Étoile SARL",
                    "Douala", "Yaoundé", Instant.now().plus(1, ChronoUnit.DAYS), StatutMission.CONFIRMEE),
            new MissionAppariee("mission-2", "tenant-bgft-douala", "axe-2", "Jean Mbarga",
                    "Douala", "Bafoussam", Instant.now().minus(2, ChronoUnit.HOURS), StatutMission.EN_COURS),
            new MissionAppariee("mission-3", "tenant-bgft-tchad", "axe-3", "Transporteur Sahel",
                    "N'Djamena", "Moundou", Instant.now().minus(1, ChronoUnit.DAYS), StatutMission.CLOTUREE)
    );

    @Override
    public Flux<MissionAppariee> listerMissionsParTenant(String tenantId) {
        return Flux.fromIterable(missions).filter(m -> m.tenantId().equals(tenantId));
    }
}
