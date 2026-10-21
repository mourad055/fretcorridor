package com.flysoft.fretcorridor.mkt.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Appel synchrone vers service-geo (autre porteur, Moteur, meme principe que
 * ServiceFltClient cote service-cap) pour resoudre l'axe correspondant a une
 * demande a la publication - ferme le bloquant audit §1.2 ("pipeline
 * marketplace -> matching mort") : sans axeId ni valeursCriteres,
 * DemandeService.publier() n'emettait jamais DemandePubliee et le cycle de
 * matching d'OPT ne se declenchait donc jamais pour les demandes marketplace.
 *
 * Resolution par nom de ville (seule donnee saisie par le client, cf
 * DemandeDto.PublierRequest) contre les hubs origine/destination de chaque
 * axe actif pour le matching (EF-GEO-03).
 *
 * Degradation gracieuse (ENF-DIS-04) : en cas d'echec (timeout, service-geo
 * indisponible) ou d'absence d'axe couvrant ces deux villes, retourne
 * Optional.empty() plutot que de propager l'exception - la demande reste
 * sauvegardee (cf Demande.axeId nullable) mais n'est pas publiee vers le
 * Moteur tant que l'axe n'est pas resolu.
 */
@Component
public class ServiceGeoClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceGeoClient.class);

    private final RestClient restClient;

    public ServiceGeoClient(@Qualifier("serviceGeoRestClient") RestClient serviceGeoRestClient) {
        this.restClient = serviceGeoRestClient;
    }

    public Optional<AxeDto> resoudreAxe(String villeDepart, String villeArrivee) {
        try {
            List<AxeDto> axes = restClient.get()
                    .uri("/api/geo/axes/actifs-matching")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<AxeDto>>() {});

            if (axes == null) {
                return Optional.empty();
            }

            return axes.stream()
                    .filter(axe -> correspond(axe.hubOrigineVille(), villeDepart)
                            && correspond(axe.hubDestinationVille(), villeArrivee))
                    .findFirst();

        } catch (RestClientException exception) {
            log.warn("Echec appel service-geo (resolution axe, {} -> {}) - "
                    + "la demande restera non publiee vers le Moteur : {}",
                    villeDepart, villeArrivee, exception.getMessage());
            return Optional.empty();
        }
    }

    private static final Pattern MARQUES_DIACRITIQUES = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    // Normalisation (21/10, suggeree par Personne 3) : "Yaounde" (saisie
    // courante sans accent, clavier mobile) doit matcher "Yaoundé" (nom du
    // hub) - auparavant un ecart d'accent faisait echouer le matching exact
    // et basculait inutilement vers le geocodage de repli.
    private boolean correspond(String villeHub, String villeSaisie) {
        return villeHub != null && villeSaisie != null
                && normaliser(villeHub).equals(normaliser(villeSaisie));
    }

    private String normaliser(String ville) {
        String sansAccents = Normalizer.normalize(ville.strip(), Normalizer.Form.NFD);
        return MARQUES_DIACRITIQUES.matcher(sansAccents).replaceAll("").toLowerCase();
    }
}
