package com.flysoft.fretcorridor.flt.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.UUID;

/**
 * Appel synchrone vers service-exe (meme porteur, Mobile) pour resoudre le
 * vehicule affecte a une mission au moment de l'ingestion d'une position -
 * ferme le canal Kafka mort "position-brute" (audit §7.1) : service-flt
 * enregistrait deja les positions mais ne les publiait jamais vers
 * service-trk (ETA, detection d'anomalies), faute de vehiculeId a inclure
 * dans PositionBruteEvent.
 *
 * GET /api/missions/{id} exige un JWT dont l'acteur est le transporteur
 * proprietaire de la mission (missionAppartenantA cote service-exe) - on
 * transmet donc l'en-tete Authorization du chauffeur qui vient d'envoyer sa
 * position, plutot que d'introduire un appel interne non authentifie
 * (meme discipline que RealCapaciteDeclarationAdapter cote gateway).
 *
 * Degradation gracieuse (ENF-DIS-04) : en cas d'echec (timeout, service-exe
 * indisponible, mission sans vehicule affecte), retourne Optional.empty() -
 * la position reste enregistree cote service-flt, seule sa publication vers
 * le Moteur est sautee ce tour-ci.
 */
@Component
public class ServiceExeClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceExeClient.class);

    private final RestClient restClient;

    public ServiceExeClient(@Qualifier("serviceExeRestClient") RestClient serviceExeRestClient) {
        this.restClient = serviceExeRestClient;
    }

    public Optional<UUID> resoudreVehicule(UUID missionId, String authHeader) {
        try {
            MissionDto mission = restClient.get()
                    .uri("/api/missions/{id}", missionId)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(MissionDto.class);

            return mission == null ? Optional.empty() : Optional.ofNullable(mission.vehiculeId());

        } catch (RestClientException exception) {
            log.warn("Echec appel service-exe (resolution vehicule, mission={}) - "
                    + "position enregistree mais non publiee vers le Moteur : {}",
                    missionId, exception.getMessage());
            return Optional.empty();
        }
    }
}
