package com.fretcorridor.gateway.domain.adm;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Appelle le service réel service-adm (comme service-pay, ce n'est pas un
 * mock : service-adm est implémenté, cf. PRD Plan d'Exécution §4.3 —
 * consultation/mutation synchrone via la gateway).
 */
public interface AdmPort {

    Flux<DossierVue> fileDeTravail(String tenantId, String delegationToken);

    Mono<DossierVue> dossier(String dossierId, String delegationToken);

    Mono<DossierVue> priseEnCharge(String dossierId, String acteurId, String delegationToken);

    Mono<DossierVue> decider(String dossierId, String decision, String motif, String acteurId, String delegationToken);

    Flux<DossierVue> declencherEscalade(String delegationToken);

    /** EF-ADM-06 : catalogue de tous les paramètres métier déjà configurés, pour la console (pas besoin de connaître la clé à l'avance). */
    Flux<ConfigurationVue> catalogueConfigurations(String delegationToken);

    Mono<ConfigurationVue> configurationCourante(String cle, String perimetre, String delegationToken);

    Flux<ConfigurationVue> historiqueConfiguration(String cle, String perimetre, String delegationToken);

    Mono<ConfigurationVue> definirConfiguration(String cle, String perimetre, String valeur, String auteur, String delegationToken);

    Flux<TenantVue> tenants(String delegationToken);

    Mono<TenantVue> creerTenant(String id, String nom, String pays, String auteur, String delegationToken);

    /** FE-ADM-04 (audit UX 2026-08-23) : édition nom/pays/statut d'un tenant existant. */
    Mono<TenantVue> modifierTenant(String id, String nom, String pays, boolean actif, String delegationToken);

    Flux<EntreeJournalAuditVue> journalAudit(String tenantId, String delegationToken);

    Mono<String> exporterJournalAudit(String tenantId, String delegationToken);

    Mono<Void> enregistrerAudit(String tenantId, String acteurId, String action, String ressource, String delegationToken);
}
