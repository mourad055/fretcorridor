package com.flysoft.fretcorridor.mkt.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Appel synchrone vers service-cap (EF-MKT-08/RG-039, audit de suivi Mobile)
 * pour reserver reellement la capacite au moment ou le chargeur accepte une
 * proposition — jusqu'ici, accepterProposition() marquait ACCEPTEE en base
 * locale sans jamais appeler decrementer(), la marketplace ne reservait donc
 * rien de reel malgre l'apparence d'un flux complet.
 *
 * Cle interne partagee (X-Internal-Service-Key, meme mecanisme que
 * service-not -> service-cap) plutot qu'un JWT utilisateur : le chargeur qui
 * accepte n'est jamais du meme tenant que le transporteur proprietaire de la
 * capacite (limitation precedemment documentee comme le pont cross-tenant
 * manquant, desormais construit).
 *
 * Contrairement a ServiceGeoClient (best-effort, degrade gracieusement) :
 * une reservation qui echoue doit faire echouer l'acceptation elle-meme —
 * accepter silencieusement sans reserver serait exactement le bug corrige
 * ici. L'appelant (DemandeService) doit laisser l'exception remonter.
 */
@Component
public class ServiceCapClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceCapClient.class);

    private final RestClient restClient;
    private final String cleInterne;

    public ServiceCapClient(@Qualifier("serviceCapRestClient") RestClient serviceCapRestClient,
                             @Value("${fretcorridor.internal.service-key}") String cleInterne) {
        this.restClient = serviceCapRestClient;
        this.cleInterne = cleInterne;
    }

    public void reserver(UUID capaciteId, BigDecimal montantKg, String cleIdempotence) {
        try {
            restClient.post()
                    .uri("/api/cap/capacites/{id}/decrement", capaciteId)
                    .header("X-Internal-Service-Key", cleInterne)
                    .body(new DecrementRequestDto(montantKg, cleIdempotence))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            log.error("Echec reservation capacite (EF-MKT-08) - capacite={}, montantKg={} : {}",
                    capaciteId, montantKg, exception.getMessage());
            throw new ReservationCapaciteException(
                    "Reservation de la capacite impossible pour le moment", exception);
        }
    }

    private record DecrementRequestDto(BigDecimal montantKg, String cleIdempotence) {
    }
}
