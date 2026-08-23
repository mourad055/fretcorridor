package com.fretcorridor.opt.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Appel synchrone interne vers service-cap pour reserver reellement le poids
 * matche des qu'une affectation L1 (Kuhn-Munkres, rang 1) est confirmee.
 *
 * BUG CORRIGE (audit de suivi, 23 aout) : le rang 1 (affectation directe,
 * AffectationConfirmeeEvent) ne decrementait jamais Capacite.capaciteResiduelleKg
 * cote service-cap - seule l'acceptation explicite d'une proposition rang 2/3
 * le faisait (ServiceCapClient.reserver, service-mkt/DemandeService, EF-MKT-08).
 * Une capacite auto-affectee etait donc a la fois marquee CapaciteEnAttente.traitee
 * (exclue de tout matching futur, cf MatchingCycleService) ET jamais reellement
 * consommee en base (cap.capacite.capacite_residuelle_kg intact) - un camion de
 * 20T aparie a 500kg voyait ses 19,5T restantes definitivement perdues pour le
 * systeme au lieu d'etre remises en file.
 *
 * Meme cle interne partagee que ServiceMatClient (X-Internal-Service-Key).
 * Contrairement a service-mkt (une acceptation utilisateur unique, un echec
 * doit annuler toute la transaction) : ce module traite plusieurs paires par
 * cycle de matching automatise - un echec reseau ponctuel sur UNE paire ne
 * doit pas faire echouer tout le cycle (ENF-DIS-04, degradation gracieuse,
 * meme raisonnement que ServiceGeoClient/ServiceMatClient dans ce module).
 * La consequence d'un echec est connue et bornee : la capacite reste
 * `capaciteResiduelleKg` intacte cote cap jusqu'au prochain decrement reussi -
 * jamais une perte de donnee silencieuse, juste une reservation en retard.
 */
@Component
public class ServiceCapClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceCapClient.class);

    private final RestClient restClient;
    private final String cleInterne;

    public ServiceCapClient(@Qualifier("serviceCapRestClient") RestClient serviceCapRestClient,
                             @Value("${fretcorridor.internal.service-key}") String cleInterne) {
        this.restClient = serviceCapRestClient;
        this.cleInterne = cleInterne;
    }

    public void reserver(UUID capaciteId, BigDecimal montantKg, String cleIdempotence) {
        try {
            restClient.post()
                    .uri("/api/cap/capacites/{id}/decrement", capaciteId)
                    .header("X-Internal-Service-Key", cleInterne)
                    .body(new DecrementRequestDto(montantKg, cleIdempotence))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            log.warn("Echec reservation capacite apres affectation L1 - capacite={}, montantKg={} : "
                            + "la capacite residuelle cote service-cap restera intacte jusqu'au prochain "
                            + "decrement reussi (ENF-DIS-04, degradation gracieuse) : {}",
                    capaciteId, montantKg, exception.getMessage());
        }
    }

    private record DecrementRequestDto(BigDecimal montantKg, String cleIdempotence) {
    }
}
