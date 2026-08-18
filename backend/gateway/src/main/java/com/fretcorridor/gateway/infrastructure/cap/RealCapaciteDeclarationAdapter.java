package com.fretcorridor.gateway.infrastructure.cap;

import com.fretcorridor.gateway.domain.cap.CapaciteDeclaree;
import com.fretcorridor.gateway.domain.cap.CapaciteDeclarationPort;
import com.fretcorridor.gateway.domain.cap.CapaciteRefuseeException;
import com.fretcorridor.gateway.domain.cap.CapServiceIndisponibleException;
import com.fretcorridor.gateway.domain.cap.DeclarationCapacite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Appelle service-cap (Mobile, port 8096) pour la déclaration de capacité
 * (EF-CAP-03/07). Pas d'en-tête d'authentification : service-cap n'a aucune
 * sécurité propre (confirmé — pas de dépendance Spring Security dans son
 * pom.xml), il n'est joignable que depuis le réseau interne via la gateway.
 */
@Component
public class RealCapaciteDeclarationAdapter implements CapaciteDeclarationPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public RealCapaciteDeclarationAdapter(WebClient.Builder webClientBuilder,
                                           @Value("${fretcorridor.service-cap.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.create().responseTimeout(TIMEOUT);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Mono<CapaciteDeclaree> declarer(DeclarationCapacite requete) {
        return webClient.post()
                .uri("/api/cap/capacites")
                .bodyValue(requete)
                .retrieve()
                .bodyToMono(CapaciteDeclaree.class)
                .onErrorMap(this::estRefus, e -> new CapaciteRefuseeException(messageDe(e)))
                .onErrorMap(e -> !(e instanceof CapaciteRefuseeException), e -> new CapServiceIndisponibleException());
    }

    private boolean estRefus(Throwable e) {
        return e instanceof WebClientResponseException wcre && wcre.getStatusCode().value() == 400;
    }

    private String messageDe(Throwable e) {
        return e instanceof WebClientResponseException wcre ? wcre.getResponseBodyAsString() : "Requête refusée";
    }
}
