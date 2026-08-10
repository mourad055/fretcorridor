package com.fretcorridor.gateway.infrastructure.geo;

import com.fretcorridor.gateway.domain.geo.Axe;
import com.fretcorridor.gateway.domain.geo.GeoPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * Appelle le service reel service-geo (Moteur, Sprint 3 livre par
 * @stevetelecom, issue #21). Meme pattern que ServicePayWebClientAdapter.
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
public class RealGeoAdapter implements GeoPort {

    private final WebClient webClient;

    public RealGeoAdapter(WebClient.Builder webClientBuilder,
                           @Value("${fretcorridor.service-geo.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Flux<Axe> listerAxesParTenant(String tenantId) {
        return webClient.get()
                .uri("/api/geo/axes")
                .retrieve()
                .bodyToFlux(AxeGeoResponse.class)
                .map(dto -> new Axe(
                        dto.id(),
                        tenantId, // tenant impose par le JWT, pas encore porte par service-geo (Phase 1)
                        dto.hubOrigineNom(),
                        dto.hubDestinationNom(),
                        0.0, // distanceKm : non expose par service-geo en Phase 1, a calculer via Valhalla en Phase 2
                        dto.visibiliteActive(),
                        dto.matchingActif(),
                        dto.paiementActif()
                ));
    }

    /** Miroir minimal du contrat AxeResponse de service-geo - champs utiles au gateway uniquement. */
    private record AxeGeoResponse(
            String id,
            String hubOrigineNom,
            String hubDestinationNom,
            boolean visibiliteActive,
            boolean matchingActif,
            boolean paiementActif
    ) {
    }
}
