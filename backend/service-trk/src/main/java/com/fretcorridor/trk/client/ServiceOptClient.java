package com.fretcorridor.trk.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.UUID;

/**
 * Appel synchrone interne vers service-opt (meme porteur, meme principe de
 * budget que GEO/MAT cf Plan d'execution S4.2) - recupere origine/destination
 * d'une mission pour le calcul d'ETA (EtaCalculator). Seul point d'entree
 * HTTP vers OPT pour tout le module TRK.
 *
 * En cas d'echec (timeout, OPT indisponible, mission pas encore persistee),
 * retourne Optional.empty() plutot que de propager l'exception : conforme
 * a ENF-DIS-04 - l'indisponibilite d'OPT ne doit jamais empecher TRK de
 * continuer a ingerer des positions, seul le calcul d'ETA de ce tour est
 * degrade (cf PositionBruteListener, qui ne publie PositionEtaEvent que si
 * l'affectation a pu etre recuperee).
 */
@Component
public class ServiceOptClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceOptClient.class);

    private final RestClient restClient;

    public ServiceOptClient(@Qualifier("serviceOptRestClient") RestClient serviceOptRestClient) {
        this.restClient = serviceOptRestClient;
    }

    /**
     * Origine/destination de la mission (= l'id de l'Affectation persistee
     * cote OPT, cf javadoc AffectationController). Optional.empty() si la
     * mission n'existe pas (404) ou si OPT est injoignable - les deux cas
     * sont traites de la meme facon cote appelant : pas d'ETA ce tour,
     * jamais de destination inventee en remplacement silencieux (meme
     * principe que ValhallaClient/ItineraireResponseDto cote OPT).
     */
    public Optional<AffectationDto> obtenirAffectation(UUID missionId) {
        try {
            AffectationDto resultat = restClient.get()
                    .uri("/api/opt/affectations/{missionId}", missionId)
                    .retrieve()
                    .body(AffectationDto.class);

            return Optional.ofNullable(resultat);

        } catch (RestClientException exception) {
            log.warn("Echec appel service-opt (affectation {}) - pas d'ETA calcule ce tour, "
                    + "mode degrade : {}", missionId, exception.getMessage());
            return Optional.empty();
        }
    }
}
