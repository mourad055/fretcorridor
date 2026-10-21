package com.flysoft.fretcorridor.mkt.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Geocodage de repli (plan §1bis, 21/10) : UNIQUEMENT quand
 * ServiceGeoClient.resoudreAxe ne trouve aucun hub correspondant au nom de
 * ville saisi (faute de frappe, alias, ville non couverte par un hub mais
 * proche d'un axe). Jamais appele depuis l'app mobile -- la politique
 * d'usage Nominatim (max 1 req/s, User-Agent identifiant, cache
 * obligatoire, licence ODbL) n'est tenable que centralisee ici, avec un
 * debit et un cache maitrises.
 *
 * Degradation gracieuse (ENF-DIS-04), meme principe que ServiceGeoClient :
 * timeout, erreur HTTP, ou aucun resultat -- Optional.empty(), jamais une
 * exception qui bloquerait la publication.
 */
@Component
public class NominatimGeocodingClient {

    private static final Logger log = LoggerFactory.getLogger(NominatimGeocodingClient.class);
    private static final long INTERVALLE_MIN_MS = 1000; // politique Nominatim : max 1 req/s

    private final RestClient restClient;
    // Cache memoire simple, sans expiration : une position geographique ne
    // change pas, et le nombre de noms de ville distincts saisis reste
    // borne (corridor regional, pas un annuaire mondial) -- pas de risque
    // de croissance non controlee en pratique.
    private final Map<String, Optional<Coordonnees>> cache = new ConcurrentHashMap<>();
    private final AtomicLong dernierAppelEpochMs = new AtomicLong(0);

    public NominatimGeocodingClient(@Qualifier("nominatimRestClient") RestClient nominatimRestClient) {
        this.restClient = nominatimRestClient;
    }

    public Optional<Coordonnees> geocoder(String requeteBrute) {
        if (requeteBrute == null || requeteBrute.isBlank()) {
            return Optional.empty();
        }
        String cle = requeteBrute.strip().toLowerCase();
        return cache.computeIfAbsent(cle, ignore -> interrogerNominatim(requeteBrute.strip()));
    }

    private Optional<Coordonnees> interrogerNominatim(String requete) {
        respecterDebit();
        try {
            List<?> brut = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", requete)
                            .queryParam("format", "jsonv2")
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .body(List.class);

            if (brut == null || brut.isEmpty() || !(brut.get(0) instanceof Map<?, ?> premiereLigne)) {
                return Optional.empty();
            }
            // Nominatim renvoie lat/lon en chaines JSON, pas en nombres.
            double latitude = Double.parseDouble(String.valueOf(premiereLigne.get("lat")));
            double longitude = Double.parseDouble(String.valueOf(premiereLigne.get("lon")));
            return Optional.of(new Coordonnees(latitude, longitude));

        } catch (RestClientException | NumberFormatException exception) {
            log.warn("Echec geocodage Nominatim (requete='{}') - repli sur aucune coordonnee : {}",
                    requete, exception.getMessage());
            return Optional.empty();
        }
    }

    // Bloque l'appelant le temps necessaire pour ne jamais depasser 1 req/s
    // vers Nominatim (politique d'usage) - acceptable ici : publier() est
    // deja un flux synchrone avec d'autres appels reseau bornes par timeout.
    private void respecterDebit() {
        synchronized (dernierAppelEpochMs) {
            long maintenant = System.currentTimeMillis();
            long attenteMs = INTERVALLE_MIN_MS - (maintenant - dernierAppelEpochMs.get());
            if (attenteMs > 0) {
                try {
                    Thread.sleep(attenteMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            dernierAppelEpochMs.set(System.currentTimeMillis());
        }
    }
}
