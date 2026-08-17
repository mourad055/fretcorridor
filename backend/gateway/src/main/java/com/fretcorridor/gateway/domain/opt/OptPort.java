package com.fretcorridor.gateway.domain.opt;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Port hexagonal : le domaine ignore que l'implémentation actuelle est un mock.
 * TODO(moteur): remplacer par l'appel réel à service-opt une fois ce service
 * livré (@stevetelecom, issue #21).
 */
public interface OptPort {

    /** ENF-MUL-01 : ne restitue jamais que les missions du tenant demandé. */
    Flux<MissionAppariee> listerMissionsParTenant(String tenantId);

    /** EF-BUR-03 : indicateurs de marché d'un axe — appelle en réalité service-bur, pas service-opt (cf. ServiceBurMissionAppparieeAdapter). */
    Mono<ObservatoireAxeVue> observatoirePourAxe(String tenantId, String axeId);

    /** EF-BUR-05, RG-087 : estimation déclarative du volume mensuel réel du marché d'un axe, saisie par un agent Bureau. */
    Mono<Void> definirEstimationMarche(String tenantId, String axeId, BigDecimal volumeMensuelEstime, String source,
                                        String acteurId);

    /** EF-BUR-07 (S) : configuration d'alertes sur seuils par l'agent — appelle service-bur. */
    Mono<AlerteSeuilVue> configurerAlerte(String tenantId, String axeId, String indicateur, String comparateur,
                                           BigDecimal seuil, String acteurId);

    Flux<AlerteSeuilVue> listerAlertes(String tenantId);

    Flux<EtatAlerteVue> etatAlertes(String tenantId);

    Mono<Void> supprimerAlerte(String id, String tenantId);
}
