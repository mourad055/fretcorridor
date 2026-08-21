package com.fretcorridor.gateway.infrastructure.geo;

import com.fretcorridor.gateway.domain.geo.Axe;
import com.fretcorridor.gateway.domain.geo.GeoPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * Appelle le service reel service-geo (Moteur, Sprint 3, @stevetelecom).
 *
 * Implementation active par defaut (production/dev) — decision d'equipe
 * 2026-08-10 (docs/adr, ADR mono-tenant GEO) : la Feuille de route V4
 * §1.1 scope la Phase 1 a un seul axe/tenant reel (BGFT), donc l'absence
 * de filtrage serveur cote service-geo n'a pas de consequence en
 * production tant que ce perimetre tient. Cet adaptateur COLLE le
 * tenantId du JWT sur chaque axe retourne par service-geo (qui n'en
 * filtre aucun lui-meme) : ce n'est PAS une garantie d'isolation
 * ENF-MUL-01 reelle, seulement une absence de risque tant qu'un seul
 * tenant existe. Des qu'un deuxieme tenant institutionnel rejoint GEO
 * (Phase 3, Plan d'Execution S18), service-geo doit exposer un vrai
 * filtre serveur (ex. GET /api/geo/axes?tenantId=) avant que ce
 * comportement ne redevienne sûr — cf. AxeControllerIsolationTest pour le
 * detail de cette limite et son suivi.
 */
@Component
@Profile("!mock-geo")
public class RealGeoAdapter implements GeoPort {

    private final WebClient webClient;

    public RealGeoAdapter(WebClient.Builder webClientBuilder,
                           @Value("${fretcorridor.service-geo.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Flux<Axe> listerAxesParTenant(String tenantId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/geo/axes")
                        .queryParam("tenantId", tenantId)
                        .build())
                .retrieve()
                .bodyToFlux(AxeGeoResponse.class)
                .map(dto -> new Axe(
                        dto.id(),
                        tenantId,
                        dto.hubOrigineNom(),
                        dto.hubDestinationNom(),
                        dto.distanceKm(),
                        dto.visibiliteActive(),
                        dto.matchingActif(),
                        dto.paiementActif()
                ));
    }

    private record AxeGeoResponse(
            String id,
            String hubOrigineNom,
            String hubDestinationNom,
            boolean visibiliteActive,
            boolean matchingActif,
            boolean paiementActif,
            double distanceKm
    ) {
    }
}
