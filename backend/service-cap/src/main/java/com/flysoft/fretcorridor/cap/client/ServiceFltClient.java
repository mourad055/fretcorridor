package com.flysoft.fretcorridor.cap.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.UUID;

/**
 * Appel synchrone vers service-flt (meme porteur, Mobile, cf Plan
 * d'execution S4.3) pour resoudre le proprietaire d'un vehicule au moment
 * de la declaration de capacite - ferme le bug S7 (transporteurId toujours
 * null cote OPT dans AffectationConfirmeeEvent).
 *
 * Degradation gracieuse (ENF-DIS-04) : en cas d'echec (timeout, service-flt
 * indisponible, vehicule non enregistre), retourne Optional.empty() plutot
 * que de propager l'exception - la declaration de capacite ne doit JAMAIS
 * echouer a cause d'une resolution transporteur en panne, seul
 * transporteurId reste null en aval.
 */
@Component
public class ServiceFltClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceFltClient.class);

    private final RestClient restClient;

    public ServiceFltClient(@Qualifier("serviceFltRestClient") RestClient serviceFltRestClient) {
        this.restClient = serviceFltRestClient;
    }

    public Optional<UUID> resoudreProprietaire(UUID vehiculeId) {
        try {
            VehiculeDto vehicule = restClient.get()
                    .uri("/api/flt/vehicules/{id}", vehiculeId)
                    .retrieve()
                    .body(VehiculeDto.class);

            return vehicule == null ? Optional.empty() : Optional.ofNullable(vehicule.proprietaireActeurId());

        } catch (RestClientException exception) {
            log.warn("Echec appel service-flt (resolution proprietaire, vehicule={}) - "
                    + "transporteurId restera null pour cette declaration : {}",
                    vehiculeId, exception.getMessage());
            return Optional.empty();
        }
    }
}
