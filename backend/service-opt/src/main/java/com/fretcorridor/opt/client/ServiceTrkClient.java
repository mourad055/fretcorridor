package com.fretcorridor.opt.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Appel synchrone interne vers service-trk (meme porteur, budget L0 ~50ms
 * cumule - meme principe que ServiceGeoClient/ServiceMatClient). Consomme
 * pour le matching en position temps reel du chauffeur (plan de
 * reorientation post-demo, partie Chauffeur point 1).
 *
 * Deux cas distincts, jamais confondus dans les logs :
 *  - 404 (aucune position jamais recue pour ce vehicule) : cas normal,
 *    log.debug, retourne null - MatchingCycleService doit alors retomber
 *    sur la position declaree (CapaciteEnAttente.getPosition()), jamais
 *    planter ni exclure le candidat silencieusement (ENF-DIS-04).
 *  - Timeout/TRK injoignable : meme retour null, mais log.warn - degradation
 *    gracieuse identique, distinction uniquement pour le diagnostic.
 */
@Component
public class ServiceTrkClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceTrkClient.class);

    private final RestClient restClient;

    public ServiceTrkClient(@Qualifier("serviceTrkRestClient") RestClient serviceTrkRestClient) {
        this.restClient = serviceTrkRestClient;
    }

    public PositionActuelleDto dernierePosition(UUID vehiculeId) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/trk/positions/derniere")
                            .queryParam("vehiculeId", vehiculeId)
                            .build())
                    .retrieve()
                    .body(PositionActuelleDto.class);

        } catch (HttpClientErrorException.NotFound notFound) {
            log.debug("Aucune position temps reel connue pour le vehicule {} - "
                    + "repli sur la position declaree.", vehiculeId);
            return null;

        } catch (RestClientException exception) {
            log.warn("Echec appel service-trk (derniere-position) - mode degrade, "
                    + "repli sur la position declaree : {}", exception.getMessage());
            return null;
        }
    }

    /**
     * Equivalent groupe de dernierePosition() - UN appel HTTP pour N
     * vehicules, plutot que N appels sequentiels (plan de reorientation,
     * position GPS temps reel dans le matching - le budget L0 ~50ms ne
     * supporte pas un aller-retour reseau par candidat).
     *
     * Un vehicule absent de la Map retournee (ou l'appel entier en echec)
     * degrade exactement comme dernierePosition() : l'appelant
     * (MatchingCycleService) doit retomber sur la position declaree,
     * jamais exclure un candidat pour ce seul motif.
     */
    public Map<UUID, PositionActuelleDto> dernieresPositions(List<UUID> vehiculeIds) {
        if (vehiculeIds == null || vehiculeIds.isEmpty()) {
            return Map.of();
        }
        try {
            PositionsBatchRequestDto requete = new PositionsBatchRequestDto(vehiculeIds);
            Map<UUID, PositionActuelleDto> resultat = restClient.post()
                    .uri("/api/trk/positions/batch")
                    .body(requete)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<UUID, PositionActuelleDto>>() {});
            return resultat == null ? Map.of() : resultat;

        } catch (RestClientException exception) {
            log.warn("Echec appel service-trk (positions/batch, {} vehicule(s)) - mode degrade, "
                    + "repli sur la position declaree pour tous : {}", vehiculeIds.size(), exception.getMessage());
            return Map.of();
        }
    }
}
