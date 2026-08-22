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
     * Index H3 de la cellule contenant un point quelconque (resolution lue
     * cote GEO dans geo.configuration_h3, jamais supposee ici). Retourne null
     * en cas d'echec - meme degradation gracieuse que les autres methodes :
     * l'appelant bascule sur son filtre de repli (rayon Haversine).
     */
    public String indexZonage(double latitude, double longitude) {
        try {
            String index = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/geo/zonage/index")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .build())
                    .retrieve()
                    .body(String.class);
            return index == null || index.isBlank() ? null : index;
        } catch (RestClientException exception) {
            log.warn("Echec appel service-geo (zonage/index) - mode degrade active : {}",
                    exception.getMessage());
            return null;
        }
    }

    /**
     * k-ring brut d'une cellule H3 : la cellule elle-meme plus ses k anneaux
     * de voisines (plafond k=3 impose cote GEO). Liste vide en cas d'echec -
     * l'appelant degrade vers son filtre Haversine plutot que de filtrer
     * tout le monde par erreur.
     */
    public List<String> kRing(String indexH3, int k) {
        try {
            String[] cellules = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/geo/zonage/k-ring")
                            .queryParam("indexH3", indexH3)
                            .queryParam("k", k)
                            .build())
                    .retrieve()
                    .body(String[].class);
            return cellules == null ? List.of() : List.of(cellules);
        } catch (RestClientException exception) {
            log.warn("Echec appel service-geo (zonage/k-ring) - mode degrade active : {}",
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

    /**
     * Detail d'un axe par id (EF-GEO-05/RG-052, Phase 4) - lit
     * parametres.conventionRepartition pour la publication de
     * RepartitionConventionnelleAppliquee. Retourne null en cas d'echec
     * (timeout, axe introuvable, GEO indisponible) - meme principe de
     * degradation gracieuse que les autres methodes de ce client :
     * l'appelant doit gerer explicitement l'absence de resultat, jamais
     * une exception qui remonterait jusqu'au cycle L1.
     */
    public AxeDetailDto axeParId(java.util.UUID axeId) {
        try {
            return restClient.get()
                    .uri("/api/geo/axes/{id}", axeId)
                    .retrieve()
                    .body(AxeDetailDto.class);
        } catch (RestClientException exception) {
            log.warn("Echec appel service-geo (axe par id) - mode degrade active, axe={} : {}",
                    axeId, exception.getMessage());
            return null;
        }
    }
}
