package com.flysoft.fretcorridor.cap.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/**
 * Appel best-effort vers service-not (meme principe que ServiceNotClient
 * cote service-mkt) pour confirmer au transporteur que sa capacite a bien
 * ete declaree — comble le meme canal jusqu'ici mort (audit de suivi
 * Mobile).
 */
@Component
public class ServiceNotClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceNotClient.class);

    private final RestClient restClient;
    private final String cleInterne;

    public ServiceNotClient(@Qualifier("serviceNotRestClient") RestClient serviceNotRestClient,
                             @Value("${fretcorridor.internal.service-key}") String cleInterne) {
        this.restClient = serviceNotRestClient;
        this.cleInterne = cleInterne;
    }

    public void notifier(UUID destinataireActeurId, String titre, String corps, String type,
                          UUID referenceId, String tenantId) {
        try {
            restClient.post()
                    .uri("/api/notifications/interne")
                    .header("X-Internal-Service-Key", cleInterne)
                    .body(new CreerNotificationRequest(
                            destinataireActeurId.toString(), titre, corps, type,
                            referenceId != null ? referenceId.toString() : null, tenantId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            log.warn("Echec appel service-not (creation notification, destinataire={}, type={}) - "
                    + "notification non creee : {}", destinataireActeurId, type, exception.getMessage());
        }
    }

    private record CreerNotificationRequest(String destinataireActeurId, String titre, String corps,
                                              String type, String referenceId, String tenantId) {
    }
}
