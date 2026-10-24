package com.fretcorridor.gateway.infrastructure.cap;

import com.fretcorridor.gateway.domain.cap.CapServiceIndisponibleException;
import com.fretcorridor.gateway.domain.cap.ConfigurerTarifChauffeur;
import com.fretcorridor.gateway.domain.cap.IntervalleTarif;
import com.fretcorridor.gateway.domain.cap.TarifChauffeur;
import com.fretcorridor.gateway.domain.cap.TarifChauffeurIntrouvableException;
import com.fretcorridor.gateway.domain.cap.TarifChauffeurPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Appelle service-cap (Mobile, port 8096) pour la tarification par chauffeur
 * (plan §1bis, PR #145) — même principe que RealCapaciteDeclarationAdapter :
 * delegationToken (JWT service-ida) retransmis en Bearer pour "mes tarifs",
 * aucun token pour /intervalle (ouvert côté service-cap).
 */
@Component
public class RealTarifChauffeurAdapter implements TarifChauffeurPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public RealTarifChauffeurAdapter(WebClient.Builder webClientBuilder,
                                      @Value("${fretcorridor.service-cap.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.create().responseTimeout(TIMEOUT);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Mono<TarifChauffeur> configurer(ConfigurerTarifChauffeur requete, String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new CapServiceIndisponibleException());
        }
        return webClient.post()
                .uri("/api/cap/tarifs")
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(requete)
                .retrieve()
                .bodyToMono(TarifChauffeur.class)
                .onErrorMap(e -> new CapServiceIndisponibleException());
    }

    @Override
    public Flux<TarifChauffeur> mesTarifs(String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new CapServiceIndisponibleException());
        }
        return webClient.get()
                .uri("/api/cap/tarifs/mes")
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(TarifChauffeur.class)
                .onErrorMap(e -> new CapServiceIndisponibleException());
    }

    @Override
    public Mono<TarifChauffeur> modifier(String id, BigDecimal tarifFcfaParKg, String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new CapServiceIndisponibleException());
        }
        return webClient.patch()
                .uri("/api/cap/tarifs/{id}", id)
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(new ModificationBody(tarifFcfaParKg))
                .retrieve()
                .bodyToMono(TarifChauffeur.class)
                .onErrorMap(this::estIntrouvable, e -> new TarifChauffeurIntrouvableException(messageDe(e)))
                .onErrorMap(e -> !(e instanceof TarifChauffeurIntrouvableException), e -> new CapServiceIndisponibleException());
    }

    @Override
    public Mono<TarifChauffeur> toggleActif(String id, String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new CapServiceIndisponibleException());
        }
        return webClient.patch()
                .uri("/api/cap/tarifs/{id}/actif", id)
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToMono(TarifChauffeur.class)
                .onErrorMap(this::estIntrouvable, e -> new TarifChauffeurIntrouvableException(messageDe(e)))
                .onErrorMap(e -> !(e instanceof TarifChauffeurIntrouvableException), e -> new CapServiceIndisponibleException());
    }

    @Override
    public Mono<Void> supprimer(String id, String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new CapServiceIndisponibleException());
        }
        return webClient.delete()
                .uri("/api/cap/tarifs/{id}", id)
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorMap(this::estIntrouvable, e -> new TarifChauffeurIntrouvableException(messageDe(e)))
                .onErrorMap(e -> !(e instanceof TarifChauffeurIntrouvableException), e -> new CapServiceIndisponibleException());
    }

    @Override
    public Mono<IntervalleTarif> intervalle(String axeId, String categorie) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/cap/tarifs/intervalle")
                        .queryParam("axeId", axeId)
                        .queryParam("categorie", categorie)
                        .build())
                .retrieve()
                .bodyToMono(IntervalleTarif.class)
                .onErrorMap(this::estIntrouvable, e -> new TarifChauffeurIntrouvableException(messageDe(e)))
                .onErrorMap(e -> !(e instanceof TarifChauffeurIntrouvableException), e -> new CapServiceIndisponibleException());
    }

    private boolean estIntrouvable(Throwable e) {
        return e instanceof WebClientResponseException wcre && wcre.getStatusCode().value() == 404;
    }

    private String messageDe(Throwable e) {
        return e instanceof WebClientResponseException wcre ? wcre.getResponseBodyAsString() : "Tarif introuvable";
    }

    private record ModificationBody(BigDecimal tarifFcfaParKg) {
    }
}
