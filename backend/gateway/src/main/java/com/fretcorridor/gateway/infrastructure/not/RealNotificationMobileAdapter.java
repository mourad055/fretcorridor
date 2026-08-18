package com.fretcorridor.gateway.infrastructure.not;

import com.fretcorridor.gateway.domain.not.NotServiceIndisponibleException;
import com.fretcorridor.gateway.domain.not.NotificationMobile;
import com.fretcorridor.gateway.domain.not.NotificationMobilePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Appelle service-not (Mobile, port 8094) pour le centre de notifications
 * (S9). Même principe de delegationToken que les autres adaptateurs Mobile.
 */
@Component
public class RealNotificationMobileAdapter implements NotificationMobilePort {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public RealNotificationMobileAdapter(WebClient.Builder webClientBuilder,
                                          @Value("${fretcorridor.service-not.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.create().responseTimeout(TIMEOUT);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Flux<NotificationMobile> mesNotifications(String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new NotServiceIndisponibleException());
        }
        return webClient.get()
                .uri("/api/notifications")
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(NotificationDto.class)
                .map(NotificationDto::versNotification)
                .onErrorMap(e -> new NotServiceIndisponibleException());
    }

    @Override
    public Mono<Integer> nombreNonLues(String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new NotServiceIndisponibleException());
        }
        return webClient.get()
                .uri("/api/notifications/non-lues/nombre")
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Integer>>() {})
                .map(m -> m.getOrDefault("nombre", 0))
                .onErrorMap(e -> new NotServiceIndisponibleException());
    }

    @Override
    public Mono<Void> marquerLue(String delegationToken, String notificationId) {
        if (delegationToken == null) {
            return Mono.error(new NotServiceIndisponibleException());
        }
        return webClient.patch()
                .uri("/api/notifications/{id}/lue", notificationId)
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorMap(e -> new NotServiceIndisponibleException());
    }

    @Override
    public Mono<Void> repondre(String delegationToken, String notificationId, boolean accepte) {
        if (delegationToken == null) {
            return Mono.error(new NotServiceIndisponibleException());
        }
        return webClient.patch()
                .uri("/api/notifications/{id}/repondre", notificationId)
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(Map.of("accepte", accepte))
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorMap(e -> new NotServiceIndisponibleException());
    }

    private record NotificationDto(String id, String titre, String corps, String type, String referenceId,
                                    Boolean lue, String dateCreation, Boolean reponseAcceptee) {
        NotificationMobile versNotification() {
            return new NotificationMobile(id, titre, corps, type, referenceId, Boolean.TRUE.equals(lue),
                    dateCreation, reponseAcceptee);
        }
    }
}
