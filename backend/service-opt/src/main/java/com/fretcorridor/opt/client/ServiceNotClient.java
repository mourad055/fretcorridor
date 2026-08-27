package com.fretcorridor.opt.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/**
 * UC-MAT-02 du CDC : notifie le transporteur qu'une mission lui est
 * proposee, attend son acceptation/refus (cf PropositionMission). Meme
 * contrat que le ServiceNotClient deja utilise cote service-mkt/service-cap
 * (POST /api/notifications/interne, X-Internal-Service-Key) -- best-effort
 * (ENF-DIS-04), une notification manquee n'annule jamais la proposition
 * elle-meme, seule sa visibilite immediate est degradee (le chauffeur la
 * verra au prochain rafraichissement de "Mes propositions").
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
            log.warn("Echec appel service-not (proposition mission, destinataire={}) - "
                    + "notification non creee, proposition inchangee : {}", destinataireActeurId, exception.getMessage());
        }
    }

    private record CreerNotificationRequest(String destinataireActeurId, String titre, String corps,
                                              String type, String referenceId, String tenantId) {
    }
}
