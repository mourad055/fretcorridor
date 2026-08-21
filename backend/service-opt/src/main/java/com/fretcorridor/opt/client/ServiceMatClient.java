package com.fretcorridor.opt.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Appel synchrone interne vers service-mat (meme porteur, budget L0/L1 ~50ms
 * cumule). Meme pattern que ServiceGeoClient : point d'entree HTTP unique
 * vers MAT pour tout le module OPT, gestion d'erreur centralisee ici plutot
 * que dupliquee a chaque appelant.
 *
 * En cas d'echec (timeout, MAT indisponible), retourne null plutot que de
 * propager l'exception (ENF-DIS-04, degradation gracieuse en cascade).
 * L'appelant (AffectationL1Service) doit signaler explicitement le mode
 * degrade plutot que d'affecter au hasard - jamais planter silencieusement.
 */
@Component
public class ServiceMatClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceMatClient.class);

    private final RestClient restClient;
    private final String cleInterne;

    public ServiceMatClient(@org.springframework.beans.factory.annotation.Qualifier("serviceMatRestClient") RestClient serviceMatRestClient,
                            @org.springframework.beans.factory.annotation.Value(
                                    "${fretcorridor.internal.service-key}") String cleInterne) {
        this.restClient = serviceMatRestClient;
        this.cleInterne = cleInterne;
    }

    /**
     * Calcule le cout composite de chaque candidat du lot face a une meme
     * demande - un seul appel HTTP pour tout le lot (meme raison que cote
     * service-mat : rester dans le budget de latence).
     *
     * FIX audit 21/08 (E2) : transporte la cle interne partagee
     * (X-Internal-Service-Key) - service-mat verifie desormais cette cle sur
     * ce endpoint de write (meme pattern que ServiceCapClient cote not,
     * PR #125).
     */
    public CoutLotResponseDto calculerCoutsLot(CoutLotRequestDto requete) {
        try {
            return restClient.post()
                    .uri("/api/mat/couts/calculer-lot")
                    .header("X-Internal-Service-Key", cleInterne)
                    .body(requete)
            .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(CoutLotResponseDto.class);

        } catch (RestClientException exception) {
            log.warn("Echec appel service-mat (calculer-lot) - mode degrade a gerer par l'appelant : {}",
                    exception.getMessage());
            return null;
        }
    }
}
