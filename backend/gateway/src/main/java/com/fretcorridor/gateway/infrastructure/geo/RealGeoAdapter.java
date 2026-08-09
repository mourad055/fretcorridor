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
 * ENF-MUL-01 : correction du 2026-08-09 (audit gateway) - le tenantId n'est
 * plus fabrique a posteriori sur chaque axe retourne. service-geo filtre
 * desormais reellement en base (GET /api/geo/axes?tenantId=...), ce gateway
 * ne fait que relayer le tenant du JWT en query param.
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
                        0.0,
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
            boolean paiementActif
    ) {
    }
}
