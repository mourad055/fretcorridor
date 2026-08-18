package com.flysoft.fretcorridor.exe.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Publié par service-exe quand l'étape LIVRAISON d'une mission est
 * confirmée par le chauffeur (POST /missions/{id}/etapes) — déclenche la
 * libération du séquestre côté service-pay (RG-078, équivalent de
 * POST /confirmation-livraison), sans passer par un appel REST synchrone
 * inter-porteurs (interdit par le Plan d'exécution §4.2/4.3, "aucune
 * exception"). Contrat proposé — voir
 * shared-contracts/asyncapi/events/mission-livree.yaml et
 * docs/DEPENDANCES_MOBILE_PHASE4.md Item A. Pas encore consommé côté
 * service-pay au moment de l'écriture (à construire côté Web).
 *
 * preuveLivraisonReference : référence opaque pour service-pay (il ne
 * valide que sa présence, pas sa nature — RG-078). Choisie ici comme l'id
 * de l'EtapeMission LIVRAISON elle-même : l'entité CDC "Preuve"
 * (photo/signature/empreinte) n'est pas encore implémentée côté
 * service-exe, donc pas de référence plus riche disponible honnêtement.
 */
public record MissionLivreeEvent(
        UUID eventId,
        UUID missionId,
        String tenantId,
        UUID transporteurId,
        String preuveLivraisonReference,
        Instant dateLivraison
) {
}
