package com.fretcorridor.gateway.domain.affiliation;

import com.fretcorridor.gateway.domain.Actor;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * S18 (Sprint 18, "Second tenant institutionnel", audit de suivi 23 aout) :
 * "Sélection de tenant au login (si multi-bureau)" — Plan d'exécution.
 * Toute implémentation appelle service-ida pour le compte de l'acteur
 * connecté avec son delegationToken (cf. AuthenticatedActor) — jamais le JWT
 * du gateway, que service-ida ne valide pas (même principe qu'IdaProfilPort).
 */
public interface AffiliationPort {

    Mono<List<TenantOption>> mesTenants(String delegationToken);

    /**
     * Réémet un Actor complet (donc un nouveau JWT gateway, cf. AuthController)
     * scopé au tenant choisi — phone/role transmis par l'appelant (déjà connus
     * du token gateway courant), le reste (nouveau delegationToken ida) vient
     * de service-ida.
     */
    Mono<Actor> selectionner(String delegationToken, String phone, com.fretcorridor.gateway.domain.Role role,
                              String tenantIdChoisi);

    /** Réservé au rôle BUREAU : rattache un transporteur/chauffeur (par téléphone) au tenant de l'appelant. */
    Mono<Void> inviter(String delegationToken, String telephoneTransporteur);
}
