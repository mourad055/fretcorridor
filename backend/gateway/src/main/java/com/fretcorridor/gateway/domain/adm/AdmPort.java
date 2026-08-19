package com.fretcorridor.gateway.domain.adm;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Appelle le service réel service-adm (comme service-pay, ce n'est pas un
 * mock : service-adm est implémenté, cf. PRD Plan d'Exécution §4.3 —
 * consultation/mutation synchrone via la gateway).
 */
public interface AdmPort {

    Flux<DossierVue> fileDeTravail(String tenantId);

    Mono<DossierVue> dossier(String dossierId);

    Mono<DossierVue> priseEnCharge(String dossierId, String acteurId);

    Mono<DossierVue> decider(String dossierId, String decision, String motif, String acteurId);

    Flux<DossierVue> declencherEscalade();

    /** EF-ADM-06 : catalogue de tous les paramètres métier déjà configurés, pour la console (pas besoin de connaître la clé à l'avance). */
    Flux<ConfigurationVue> catalogueConfigurations();

    Mono<ConfigurationVue> configurationCourante(String cle, String perimetre);

    Flux<ConfigurationVue> historiqueConfiguration(String cle, String perimetre);

    Mono<ConfigurationVue> definirConfiguration(String cle, String perimetre, String valeur, String auteur);

    Flux<TenantVue> tenants();

    Mono<TenantVue> creerTenant(String id, String nom, String pays, String auteur);

    Flux<EntreeJournalAuditVue> journalAudit(String tenantId);

    Mono<String> exporterJournalAudit(String tenantId);

    Mono<Void> enregistrerAudit(String tenantId, String acteurId, String action, String ressource);
}
