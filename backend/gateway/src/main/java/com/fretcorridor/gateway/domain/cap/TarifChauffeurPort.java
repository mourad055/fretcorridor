package com.fretcorridor.gateway.domain.cap;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Tarification par chauffeur (plan §1bis, PR #145 côté service-cap) — appel
 * réel, même principe que CapaciteDeclarationPort. "Mes tarifs" exige le
 * delegationToken du chauffeur/transporteur connecté ; intervalle() est
 * ouvert côté service-cap (aucune donnée personnelle), consommé par le
 * client final pour l'écran Estimation.
 */
public interface TarifChauffeurPort {
    Mono<TarifChauffeur> configurer(ConfigurerTarifChauffeur requete, String delegationToken);

    Flux<TarifChauffeur> mesTarifs(String delegationToken);

    Mono<TarifChauffeur> modifier(String id, BigDecimal tarifFcfaParKg, String delegationToken);

    Mono<TarifChauffeur> toggleActif(String id, String delegationToken);

    Mono<Void> supprimer(String id, String delegationToken);

    Mono<IntervalleTarif> intervalle(String axeId, String categorie);
}
