package com.fretcorridor.gateway.domain.pay;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Port hexagonal vers service-pay — contrairement aux autres périmètres
 * consommés cette Phase 1 (GEO, OPT, TRK, EXE), service-pay appartient à ce
 * même périmètre et est réellement implémenté : cet adaptateur appelle le
 * service réel (WebClient), ce n'est pas un mock.
 */
public interface PayReadPort {

    Flux<EcritureVue> ecrituresDuTransporteur(String transporteurId);

    Flux<EcritureVue> rapportDuTenant(String tenantId);

    Flux<DeclarationEspecesVue> paiementsEspecesDuTenant(String tenantId);

    // S14 Item B (Volet A, Chauffeur) : erreur MissionIntrouvableException si
    // le chargeur n'a pas encore choisi de moyen pour cette mission (404 côté
    // service-pay).
    Mono<ModePaiementChoisi> modePaiementChoisi(String missionId);
}
