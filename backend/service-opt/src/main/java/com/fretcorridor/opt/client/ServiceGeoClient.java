package com.fretcorridor.opt.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Appel synchrone interne vers service-geo (meme porteur, budget L0 ~50ms).
 * Seul point d'entree HTTP vers GEO pour tout le module OPT - centralise ici
 * pour que la gestion d'erreur/timeout soit uniforme, jamais dupliquee ailleurs.
 *
 * En cas d'echec (timeout, GEO indisponible), retourne une liste vide plutot que
 * de propager l'exception : conforme a ENF-DIS-04, "l'indisponibilite du moteur
 * ne doit jamais empecher consultation, publication ou suivi" - ici c'est GEO qui
 * serait indisponible, mais le principe de degradation gracieuse s'applique de la
 * meme facon en cascade (OPT doit rester utilisable, au pire avec un resultat vide
 * et un mode degrade signale par l'appelant, cf EF-MAT-12).
 */
@Component
public class ServiceGeoClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceGeoClient.class);

    private final RestClient restClient;

    public ServiceGeoClient(@org.springframework.beans.factory.annotation.Qualifier("serviceGeoRestClient") RestClient serviceGeoRestClient) {
        this.restClient = serviceGeoRestClient;
    }

    /**
     * Hubs presents dans la cellule H3 d'un point donne (origine d'une demande)
     * ou dans ses k anneaux de voisines - coeur du filtre L0.
     */
    public List<HubProcheDto> hubsProches(double latitude, double longitude, int k) {
        try {
            HubProcheDto[] resultat = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/geo/zonage/hubs-proches")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("k", k)
                            .build())
                    .retrieve()
                    .body(HubProcheDto[].class);

            return resultat == null ? List.of() : List.of(resultat);

        } catch (RestClientException exception) {
            // Timeout, connexion refusee, ou reponse HTTP en erreur : on logue en WARN
            // (pas ERROR - c'est un mode degrade gere, pas un crash) et on degrade
            // proprement plutot que de faire planter l'appelant.
            log.warn("Echec appel service-geo (hubs-proches) - mode degrade active : {}",
                    exception.getMessage());
            return List.of();
        }
    }

    /**
     * Axes ou le matching est actif (EF-GEO-03) - c'est sur cette liste que
     * MatchingCycleService boucle pour declencher un cycle par axe. Un axe
     * absent de cette liste n'est jamais propose au matching, meme s'il a des
     * capacites/demandes en attente (EF-MAT-01, "actif seulement si l'axe
     * l'autorise").
     */
    public List<AxeActifDto> axesActifsMatching() {
        try {
            AxeActifDto[] resultat = restClient.get()
                    .uri("/api/geo/axes/actifs-matching")
                    .retrieve()
                    .body(AxeActifDto[].class);

            return resultat == null ? List.of() : List.of(resultat);

        } catch (RestClientException exception) {
            log.warn("Echec appel service-geo (axes-actifs-matching) - mode degrade active, "
                    + "aucun cycle de matching ne sera declenche ce tour : {}", exception.getMessage());
            return List.of();
        }
    }
}
