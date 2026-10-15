package com.fretcorridor.gateway.infrastructure.cap;

import com.fretcorridor.gateway.domain.cap.CapServiceIndisponibleException;
import com.fretcorridor.gateway.domain.cap.PropositionCap;
import com.fretcorridor.gateway.domain.cap.PropositionCapPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * UC-MAT-02/diffusion-course : appelle service-cap (Mobile, port 8096),
 * même patron d'authentification que RealCapaciteDeclarationAdapter
 * (delegationToken en Bearer, service-cap résout transporteurId depuis
 * le JWT lui-même, jamais transmis en clair par la gateway).
 */
@Component
public class RealPropositionCapAdapter implements PropositionCapPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public RealPropositionCapAdapter(WebClient.Builder webClientBuilder,
                                      @Value("${fretcorridor.service-cap.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.create().responseTimeout(TIMEOUT);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Flux<PropositionCap> mesPropositions(String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new CapServiceIndisponibleException());
        }
        return webClient.get()
                .uri("/api/cap/propositions/mes")
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(PropositionCap.class)
                .onErrorMap(e -> new CapServiceIndisponibleException());
    }

    @Override
    public Mono<Void> accepter(UUID affectationId, UUID demandeId, UUID capaciteId, String delegationToken) {
        return repondre("accepter", affectationId, demandeId, capaciteId, delegationToken);
    }

    @Override
    public Mono<Void> refuser(UUID affectationId, UUID demandeId, UUID capaciteId, String delegationToken) {
        return repondre("refuser", affectationId, demandeId, capaciteId, delegationToken);
    }

    private Mono<Void> repondre(String action, UUID affectationId, UUID demandeId, UUID capaciteId,
                                 String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new CapServiceIndisponibleException());
        }
        return webClient.post()
                .uri("/api/cap/propositions/{id}/" + action, affectationId)
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(Map.of("demandeId", demandeId, "capaciteId", capaciteId))
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorMap(e -> new CapServiceIndisponibleException());
    }
}
