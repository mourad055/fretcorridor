package com.fretcorridor.gateway.infrastructure.not;

import com.fretcorridor.gateway.domain.not.CanalNotification;
import com.fretcorridor.gateway.domain.not.NotServiceIndisponibleException;
import com.fretcorridor.gateway.domain.not.Notification;
import com.fretcorridor.gateway.domain.not.NotificationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Centre de notifications Bureau (S9) — appel réel à service-not via le
 * delegationToken service-ida (même principe que RealNotificationMobileAdapter).
 */
@Component
public class RealNotAdapter implements NotificationPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public RealNotAdapter(WebClient.Builder webClientBuilder,
                          @Value("${fretcorridor.service-not.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.create().responseTimeout(TIMEOUT);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Flux<Notification> listerNotificationsParTenant(String tenantId, String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new NotServiceIndisponibleException());
        }
        return webClient.get()
                .uri("/api/notifications")
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(NotificationDto.class)
                .map(dto -> versNotification(tenantId, dto))
                .onErrorMap(e -> new NotServiceIndisponibleException());
    }

    private Notification versNotification(String tenantId, NotificationDto dto) {
        return new Notification(
                dto.id(),
                tenantId,
                CanalNotification.IN_APP,
                "Centre de notifications",
                dto.titre(),
                dto.corps(),
                dto.dateCreation() != null ? dto.dateCreation().atOffset(ZoneOffset.UTC).toInstant() : Instant.now()
        );
    }

    private record NotificationDto(String id, String titre, String corps, String type, String referenceId,
                                   Boolean lue, LocalDateTime dateCreation, Boolean reponseAcceptee) {
    }
}
