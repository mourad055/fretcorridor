package com.fretcorridor.gateway.infrastructure.opt;

import com.fretcorridor.gateway.domain.opt.AlerteSeuilVue;
import com.fretcorridor.gateway.domain.opt.EtatAlerteVue;
import com.fretcorridor.gateway.domain.opt.MissionAppariee;
import com.fretcorridor.gateway.domain.opt.ObservatoireAxeVue;
import com.fretcorridor.gateway.domain.opt.OptPort;
import com.fretcorridor.gateway.domain.opt.StatutMission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Appelle service-bur, PAS service-opt (Moteur) directement — opt-api.yaml
 * interdit explicitement tout consommateur cross-porteur en REST synchrone
 * ("le flux OPT -> Mobile passe exclusivement par événements Kafka
 * asynchrones"). service-bur (Web) matérialise la vue Bureau en consommant
 * l'événement AffectationConfirmee (cf.
 * AffectationConfirmeeListener/MissionAppparieeController côté service-bur),
 * et c'est cette vue déjà matérialisée que cet adaptateur consulte.
 *
 * Deux limites acceptées, documentées plutôt que masquées (cf.
 * docs/ROADMAP_INTEGRATION_gateway.md, item #2) :
 * - {@code transporteurNom} n'est PAS un nom : AffectationConfirmee ne porte
 *   qu'un transporteurId (UUID), jamais résolu ici — obtenir le vrai nom
 *   exigerait un appel supplémentaire vers service-ida, hors périmètre de
 *   cette passe.
 * - {@code statut} vaut toujours CONFIRMEE : aucun événement ne porte
 *   encore les transitions EN_COURS/CLOTUREE (ni AffectationConfirmee, ni
 *   aucun autre contrat de shared-contracts/asyncapi/ à ce jour).
 */
@Component
public class ServiceBurMissionAppparieeAdapter implements OptPort {

    private final WebClient webClient;

    public ServiceBurMissionAppparieeAdapter(WebClient.Builder webClientBuilder,
                                              @Value("${fretcorridor.service-bur.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Flux<MissionAppariee> listerMissionsParTenant(String tenantId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/bur/missions-appariees")
                        .queryParam("tenantId", tenantId)
                        .build())
                .retrieve()
                .bodyToFlux(MissionAppparieeBurResponse.class)
                .map(dto -> new MissionAppariee(
                        dto.missionId(),
                        tenantId,
                        dto.axeId(),
                        dto.transporteurId(), // pas un nom — cf Javadoc de la classe
                        dto.origineNom(),
                        dto.destinationNom(),
                        dto.confirmeeLe(),
                        StatutMission.CONFIRMEE // seul statut connaissable depuis cet événement
                ));
    }

    @Override
    public Mono<ObservatoireAxeVue> observatoirePourAxe(String tenantId, String axeId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/bur/observatoire")
                        .queryParam("tenantId", tenantId)
                        .queryParam("axeId", axeId)
                        .build())
                .retrieve()
                .bodyToMono(ObservatoireAxeVue.class);
    }

    @Override
    public Mono<Void> definirEstimationMarche(String tenantId, String axeId, BigDecimal volumeMensuelEstime,
                                               String source, String acteurId) {
        return webClient.put()
                .uri("/api/v1/bur/estimation-marche")
                .bodyValue(new DefinirEstimationMarcheBurRequest(tenantId, axeId, volumeMensuelEstime, source, acteurId))
                .retrieve()
                .bodyToMono(Void.class);
    }

    @Override
    public Mono<AlerteSeuilVue> configurerAlerte(String tenantId, String axeId, String indicateur, String comparateur,
                                                  BigDecimal seuil, String acteurId) {
        return webClient.post()
                .uri("/api/v1/bur/alertes")
                .bodyValue(new ConfigurerAlerteBurRequest(tenantId, axeId, indicateur, comparateur, seuil, acteurId))
                .retrieve()
                .bodyToMono(AlerteSeuilVue.class);
    }

    @Override
    public Flux<AlerteSeuilVue> listerAlertes(String tenantId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/bur/alertes").queryParam("tenantId", tenantId).build())
                .retrieve()
                .bodyToFlux(AlerteSeuilVue.class);
    }

    @Override
    public Flux<EtatAlerteVue> etatAlertes(String tenantId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/bur/alertes/etat").queryParam("tenantId", tenantId).build())
                .retrieve()
                .bodyToFlux(EtatAlerteVue.class);
    }

    @Override
    public Mono<Void> supprimerAlerte(String id, String tenantId) {
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/bur/alertes/{id}").queryParam("tenantId", tenantId).build(id))
                .retrieve()
                .bodyToMono(Void.class);
    }

    private record ConfigurerAlerteBurRequest(String tenantId, String axeId, String indicateur, String comparateur,
                                               BigDecimal seuil, String acteurId) {
    }

    private record DefinirEstimationMarcheBurRequest(String tenantId, String axeId, BigDecimal volumeMensuelEstime,
                                                       String source, String acteurId) {
    }

    /** Miroir minimal du contrat MissionAppparieeResponse de service-bur. */
    private record MissionAppparieeBurResponse(
            String missionId,
            String axeId,
            String transporteurId,
            String origineNom,
            String destinationNom,
            java.math.BigDecimal prixTransport,
            String devise,
            Instant confirmeeLe
    ) {
    }
}
