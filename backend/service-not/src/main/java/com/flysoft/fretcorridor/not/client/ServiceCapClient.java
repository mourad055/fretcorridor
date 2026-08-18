package com.flysoft.fretcorridor.not.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.UUID;

/**
 * Appel synchrone vers service-cap (même porteur, Mobile) pour résoudre le
 * transporteur d'une capacité (S12, retour à vide) — la même grandeur que
 * ServiceFltClient (service-cap -> service-flt) mais dans l'autre sens.
 *
 * Dégradation gracieuse : en cas d'échec (timeout, service-cap indisponible,
 * capacité introuvable), retourne Optional.empty() plutôt que de propager
 * l'exception — un échec de résolution ne doit jamais faire perdre
 * l'événement Kafka (voir PropositionRetourAVideListener : sans
 * transporteurId, la notification est simplement sautée avec un WARN,
 * pas de retry silencieux qui masquerait le problème).
 */
@Component
public class ServiceCapClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceCapClient.class);

    private final RestClient restClient;

    public ServiceCapClient(@Qualifier("serviceCapRestClient") RestClient serviceCapRestClient) {
        this.restClient = serviceCapRestClient;
    }

    public Optional<UUID> resoudreTransporteur(UUID capaciteId) {
        try {
            CapaciteDto capacite = restClient.get()
                    .uri("/api/cap/capacites/{id}", capaciteId)
                    .retrieve()
                    .body(CapaciteDto.class);

            return capacite == null ? Optional.empty() : Optional.ofNullable(capacite.transporteurId());

        } catch (RestClientException exception) {
            log.warn("Echec appel service-cap (resolution transporteur, capacite={}) - "
                    + "notification de retour a vide non envoyee : {}",
                    capaciteId, exception.getMessage());
            return Optional.empty();
        }
    }
}
