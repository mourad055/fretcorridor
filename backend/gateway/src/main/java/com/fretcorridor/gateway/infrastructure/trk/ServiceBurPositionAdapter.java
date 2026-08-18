package com.fretcorridor.gateway.infrastructure.trk;

import com.fretcorridor.gateway.domain.trk.PositionVehicule;
import com.fretcorridor.gateway.domain.trk.TrkPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

/**
 * Appelle service-bur, PAS service-trk (Moteur) directement — service-trk
 * n'expose d'ailleurs aucune API REST, tout y est événementiel (Kafka
 * position-eta). service-bur (Web) matérialise la vue Bureau en consommant
 * cet événement (cf. PositionEtaListener/PositionController côté
 * service-bur), et c'est cette vue déjà matérialisée que cet adaptateur
 * consulte. Même patron que ServiceBurMissionAppparieeAdapter (cf.
 * docs/ROADMAP_INTEGRATION_gateway.md, item #4).
 *
 * Limite acceptée, documentée plutôt que masquée : {@code vehiculeLabel}
 * n'est PAS un libellé lisible : PositionEtaEvent ne porte qu'un
 * vehiculeId (UUID), jamais résolu ici — obtenir la vraie plaque/label
 * exigerait un appel supplémentaire vers un référentiel véhicules, hors
 * périmètre de cette passe.
 */
@Component
public class ServiceBurPositionAdapter implements TrkPort {

    private final WebClient webClient;

    public ServiceBurPositionAdapter(WebClient.Builder webClientBuilder,
                                      @Value("${fretcorridor.service-bur.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Flux<PositionVehicule> listerPositionsParTenant(String tenantId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/bur/positions")
                        .queryParam("tenantId", tenantId)
                        .build())
                .retrieve()
                .bodyToFlux(PositionBurResponse.class)
                .map(dto -> new PositionVehicule(
                        dto.missionId().toString(),
                        tenantId,
                        dto.vehiculeId().toString(), // pas un libellé — cf Javadoc de la classe
                        dto.latitude(),
                        dto.longitude(),
                        dto.capturedLe()
                ));
    }

    /** Miroir minimal du contrat PositionResponse de service-bur. */
    private record PositionBurResponse(
            UUID missionId,
            UUID vehiculeId,
            double latitude,
            double longitude,
            Instant capturedLe
    ) {
    }
}
