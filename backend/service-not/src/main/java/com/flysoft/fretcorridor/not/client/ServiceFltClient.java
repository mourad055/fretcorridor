package com.flysoft.fretcorridor.not.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.UUID;

/**
 * Appel synchrone vers service-flt (meme porteur, Mobile, meme principe que
 * ServiceCapClient) pour resoudre le proprietaire d'un vehicule au moment
 * d'une AlerteEcart - ferme le canal Kafka mort "alerte-ecart" (audit §7.1) :
 * service-trk publiait deja l'evenement, mais aucun consommateur n'existait.
 *
 * Degradation gracieuse (ENF-DIS-04) : en cas d'echec (timeout, service-flt
 * indisponible, vehicule non enregistre), retourne Optional.empty() plutot
 * que de propager l'exception - l'alerte est simplement non notifiee ce
 * tour-ci (WARN), pas de retry silencieux qui masquerait le probleme.
 */
@Component
public class ServiceFltClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceFltClient.class);

    private final RestClient restClient;
    private final String cleInterne;

    public ServiceFltClient(@Qualifier("serviceFltRestClient") RestClient serviceFltRestClient,
                             @Value("${fretcorridor.internal.service-key}") String cleInterne) {
        this.restClient = serviceFltRestClient;
        this.cleInterne = cleInterne;
    }

    public Optional<UUID> resoudreProprietaire(UUID vehiculeId) {
        try {
            // Audit de suivi Mobile : cet appel partait sans aucun header
            // d'authentification alors que l'endpoint exige un JWT ou cette
            // cle interne depuis la fermeture du bloquant "vehicule public,
            // sans filtre tenant" (VehiculeController) -- 401 systematique,
            // canal alerte-ecart silencieusement mort en pratique.
            VehiculeDto vehicule = restClient.get()
                    .uri("/api/flt/vehicules/{id}", vehiculeId)
                    .header("X-Internal-Service-Key", cleInterne)
                    .retrieve()
                    .body(VehiculeDto.class);

            return vehicule == null ? Optional.empty() : Optional.ofNullable(vehicule.proprietaireActeurId());

        } catch (RestClientException exception) {
            log.warn("Echec appel service-flt (resolution proprietaire, vehicule={}) - "
                    + "alerte d'ecart non notifiee : {}",
                    vehiculeId, exception.getMessage());
            return Optional.empty();
        }
    }
}
