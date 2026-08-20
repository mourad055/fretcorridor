package com.fretcorridor.gateway.infrastructure.agent;

import com.fretcorridor.gateway.domain.agent.AgentEnrolementPort;
import com.fretcorridor.gateway.domain.agent.AgentServiceIndisponibleException;
import com.fretcorridor.gateway.domain.agent.Enrolement;
import com.fretcorridor.gateway.domain.agent.EnrolementIntrouvableException;
import com.fretcorridor.gateway.domain.agent.EnrolementRefuseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Map;

/**
 * Appelle service-ida (Mobile) pour l'enrôlement assisté par agent
 * (UC-IDA-03/EF-IDA-06). Même principe de delegationToken que
 * RealIdaProfilAdapter — jamais le JWT du gateway.
 */
@Component
public class RealAgentEnrolementAdapter implements AgentEnrolementPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;

    public RealAgentEnrolementAdapter(WebClient.Builder webClientBuilder,
                                       @Value("${fretcorridor.service-ida.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.create().responseTimeout(TIMEOUT);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Mono<Enrolement> initier(String delegationToken, String telephone, String typeActeur,
                                     double latitude, double longitude, String idempotencyKey) {
        if (delegationToken == null) {
            return Mono.error(new AgentServiceIndisponibleException());
        }
        return webClient.post()
                .uri("/api/agent/enrolements")
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(Map.of(
                        "telephone", telephone,
                        "typeActeur", typeActeur,
                        "latitude", latitude,
                        "longitude", longitude,
                        "idempotencyKey", idempotencyKey))
                .retrieve()
                .bodyToMono(EnrolementDto.class)
                .map(EnrolementDto::versEnrolement)
                .transform(this::gererErreurs);
    }

    @Override
    public Mono<Enrolement> activer(String delegationToken, String enrolementId, String otp, String codePin) {
        if (delegationToken == null) {
            return Mono.error(new AgentServiceIndisponibleException());
        }
        return webClient.post()
                .uri("/api/agent/enrolements/{id}/activation", enrolementId)
                .bodyValue(Map.of("otp", otp, "codePin", codePin))
                .retrieve()
                .onStatus(status -> status.value() == 404, r -> Mono.error(new EnrolementIntrouvableException()))
                .bodyToMono(EnrolementDto.class)
                .map(EnrolementDto::versEnrolement)
                .transform(this::gererErreurs);
    }

    private Mono<Enrolement> gererErreurs(Mono<Enrolement> mono) {
        return mono
                .onErrorMap(this::estRefus, e -> new EnrolementRefuseException(messageDe(e)))
                .onErrorMap(e -> !(e instanceof EnrolementRefuseException) && !(e instanceof EnrolementIntrouvableException),
                        e -> new AgentServiceIndisponibleException());
    }

    private boolean estRefus(Throwable e) {
        return e instanceof WebClientResponseException wcre && wcre.getStatusCode().value() == 400;
    }

    private String messageDe(Throwable e) {
        return e instanceof WebClientResponseException wcre ? wcre.getResponseBodyAsString() : "Requête refusée";
    }

    private record EnrolementDto(String enrolementId, String telephone, String typeActeur, String statut) {
        Enrolement versEnrolement() {
            return new Enrolement(enrolementId, telephone, typeActeur, statut);
        }
    }
}
