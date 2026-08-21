package com.fretcorridor.gateway.infrastructure.flt;

import com.fretcorridor.gateway.domain.flt.FltServiceIndisponibleException;
import com.fretcorridor.gateway.domain.flt.PositionEnvoi;
import com.fretcorridor.gateway.domain.flt.PositionPort;
import com.fretcorridor.gateway.domain.flt.PositionRefuseeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;

/**
 * Appelle service-flt (Mobile, port 8083) pour l'envoi de positions GPS
 * (EF-TRK-01, Sprint 6). Même principe de delegationToken que
 * RealAgentEnrolementAdapter — service-flt valide les JWT service-ida.
 */
@Component
public class RealPositionAdapter implements PositionPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public RealPositionAdapter(WebClient.Builder webClientBuilder,
                                @Value("${fretcorridor.service-flt.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.create().responseTimeout(TIMEOUT);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Mono<Void> envoyer(String delegationToken, PositionEnvoi position) {
        if (delegationToken == null) {
            return Mono.error(new FltServiceIndisponibleException());
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("missionId", position.missionId());
        body.put("latitude", position.latitude());
        body.put("longitude", position.longitude());
        body.put("horodatage", position.horodatage());
        // FIX audit 21/08 : champs optionnels du contrat position-brute
        // (eventId à la capture, sourceCapture enum, precisionMetres).
        if (position.eventId() != null) {
            body.put("eventId", position.eventId());
        }
        if (position.sourceCapture() != null) {
            body.put("sourceCapture", position.sourceCapture());
        }
        if (position.precisionMetres() != null) {
            body.put("precisionMetres", position.precisionMetres());
        }
        return webClient.post()
                .uri("/api/positions")
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorMap(this::estRefus, e -> new PositionRefuseeException(messageDe(e)))
                .onErrorMap(e -> !(e instanceof PositionRefuseeException), e -> new FltServiceIndisponibleException());
    }

    private boolean estRefus(Throwable e) {
        return e instanceof WebClientResponseException wcre && wcre.getStatusCode().value() == 400;
    }

    private String messageDe(Throwable e) {
        return e instanceof WebClientResponseException wcre ? wcre.getResponseBodyAsString() : "Requête refusée";
    }
}
