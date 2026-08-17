package com.fretcorridor.gateway.infrastructure.opt;

import com.fretcorridor.gateway.domain.opt.AlerteSeuilVue;
import com.fretcorridor.gateway.domain.opt.EtatAlerteVue;
import com.fretcorridor.gateway.domain.opt.MissionAppariee;
import com.fretcorridor.gateway.domain.opt.ObservatoireAxeVue;
import com.fretcorridor.gateway.domain.opt.OptPort;
import com.fretcorridor.gateway.domain.opt.StatutMission;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Override
    public Mono<ObservatoireAxeVue> observatoirePourAxe(String tenantId, String axeId) {
        return Mono.just(new ObservatoireAxeVue(axeId, 3, false, null, null, null, null, null, null, null));
    }

    private final java.util.Map<String, BigDecimal> estimationsMarche = new java.util.HashMap<>();

    @Override
    public Mono<Void> definirEstimationMarche(String tenantId, String axeId, BigDecimal volumeMensuelEstime,
                                               String source, String acteurId) {
        estimationsMarche.put(tenantId + ":" + axeId, volumeMensuelEstime);
        return Mono.empty();
    }

    private final List<AlerteSeuilVue> alertes = new ArrayList<>();

    @Override
    public Mono<AlerteSeuilVue> configurerAlerte(String tenantId, String axeId, String indicateur, String comparateur,
                                                  BigDecimal seuil, String acteurId) {
        AlerteSeuilVue alerte = new AlerteSeuilVue(UUID.randomUUID().toString(), axeId, indicateur, comparateur,
                seuil, acteurId, Instant.now());
        alertes.add(alerte);
        return Mono.just(alerte);
    }

    @Override
    public Flux<AlerteSeuilVue> listerAlertes(String tenantId) {
        return Flux.fromIterable(alertes);
    }

    @Override
    public Flux<EtatAlerteVue> etatAlertes(String tenantId) {
        return Flux.fromIterable(alertes).map(a -> new EtatAlerteVue(a, false, false, null));
    }

    @Override
    public Mono<Void> supprimerAlerte(String id, String tenantId) {
        alertes.removeIf(a -> a.id().equals(id));
        return Mono.empty();
    }
}
