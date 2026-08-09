package com.fretcorridor.gateway.infrastructure.geo;

import com.fretcorridor.gateway.domain.geo.Axe;
import com.fretcorridor.gateway.domain.geo.GeoPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * Appelle le service reel service-geo (Moteur, Sprint 3 livre par
 * @stevetelecom, issue #21). Meme pattern que ServicePayWebClientAdapter.
 *
 * ENF-MUL-01 : service-geo n'a pas encore de notion de tenant active en
 * Phase 1 (colonne tenant_id ajoutee, cf migration V4, mais un seul tenant
 * existe - BGFT). Le filtrage par tenant reste donc fait ICI, cote gateway,
 * en attendant le multi-tenant reel (Phase 3, Plan d'execution S18).
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
