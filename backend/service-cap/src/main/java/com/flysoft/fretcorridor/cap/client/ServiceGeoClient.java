package com.flysoft.fretcorridor.cap.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Appel synchrone vers service-geo (autre porteur, Moteur, meme principe que
 * ServiceFltClient cote service-cap) pour resoudre les coefficients de poids
 * taxable specifiques a un axe (RG-101, CDC §8.3.3) : "le systeme ne doit
 * jamais appliquer une seule valeur globale, le parametrage est versionne
 * par tenant et par axe". Les coefficients vivent dans Axe.parametres (meme
 * mecanisme que detourMaxDistanceKm/EF-MAT-10 cote service-opt), un axe
 * appartenant a exactement un tenant -- scoper par axe scope donc aussi par
 * tenant.
 *
 * Degradation gracieuse (ENF-DIS-04) : en cas d'echec (timeout, service-geo
 * indisponible), axe introuvable, ou cle absente des parametres, retourne
 * Optional.empty() -- CalculateurPoidsTaxable retombe alors sur les valeurs
 * de reference globales (fretcorridor.cap.coefficient-*), jamais un echec de
 * la declaration de capacite pour ce motif.
 */
@Component
public class ServiceGeoClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceGeoClient.class);

    private final RestClient restClient;

    public ServiceGeoClient(@Qualifier("serviceGeoRestClient") RestClient serviceGeoRestClient) {
        this.restClient = serviceGeoRestClient;
    }

    public Optional<Map<String, Object>> parametresAxe(UUID axeId) {
        if (axeId == null) {
            return Optional.empty();
        }
        try {
            AxeDto axe = restClient.get()
                    .uri("/api/geo/axes/{id}", axeId)
                    .retrieve()
                    .body(AxeDto.class);

            return (axe == null || axe.parametres() == null) ? Optional.empty() : Optional.of(axe.parametres());

        } catch (RestClientException exception) {
            log.warn("Echec appel service-geo (resolution parametres axe={}) - "
                    + "repli sur les coefficients de reference globaux (RG-101) : {}",
                    axeId, exception.getMessage());
            return Optional.empty();
        }
    }
}
