package com.fretcorridor.opt.client;

import com.fretcorridor.opt.config.ValhallaClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Appel au service d'itineraires Valhalla (integration externe, cf Plan
 * d'execution S1 : OSRM/OpenStreetMap - Valhalla en est l'implementation
 * retenue). Utilise en L1 (tarification/ETA initial) et par TRK (itineraire
 * retenu pour le calcul d'ecart de trajectoire).
 *
 * Meme principe de degradation gracieuse que ServiceGeoClient/ServiceMatClient
 * (ENF-DIS-04) : en cas d'echec, retourne null plutot que de propager
 * l'exception. Contrairement a GEO/MAT (meme porteur, latence ~ms), Valhalla
 * est une dependance externe reseau - le timeout plus genereux (cf
 * ValhallaClientProperties) reste borne pour ne jamais bloquer OPT
 * indefiniment.
 *
 * Ne construit/ne lit AUCUN JSON Valhalla directement : toute la traduction
 * DTO interne <-> contrat Valhalla passe par ValhallaRequestMapper, pour que
 * cette classe reste lisible et testable sans mock HTTP complexe.
 */
@Component
public class ValhallaClient {

    private static final Logger log = LoggerFactory.getLogger(ValhallaClient.class);

    private final RestClient restClient;
    private final ValhallaRequestMapper mapper;
    private final double margeRatioEta;

    public ValhallaClient(@Qualifier("valhallaRestClient") RestClient valhallaRestClient,
                           ValhallaRequestMapper mapper,
                           ValhallaClientProperties properties) {
        this.restClient = valhallaRestClient;
        this.mapper = mapper;
        this.margeRatioEta = properties.getMargeRatioEta();
    }

    /**
     * Calcule l'itineraire routier compatible camion entre une suite de points.
     * Retourne null en cas d'echec (timeout, Valhalla indisponible, itineraire
     * introuvable, reponse inexploitable) - l'appelant doit gerer explicitement
     * ce cas en mode degrade, jamais improviser une distance a vol d'oiseau en
     * remplacement silencieux.
     */
    public ItineraireResponseDto calculerItineraire(ItineraireRequestDto requete) {
        ValhallaRouteRequest requeteValhalla = mapper.versValhalla(requete);

        try {
            ValhallaRouteResponse reponse = restClient.post()
                    .uri("/route")
                    .body(requeteValhalla)
                    .retrieve()
                    .body(ValhallaRouteResponse.class);

            if (reponse == null) {
                log.warn("Reponse Valhalla vide - mode degrade a gerer par l'appelant");
                return null;
            }
            return mapper.mapVersDto(reponse, margeRatioEta);

        } catch (RestClientException exception) {
            log.warn("Echec appel Valhalla (calcul itineraire) - mode degrade a gerer par l'appelant : {}",
                    exception.getMessage());
            return null;
        }
    }
}
