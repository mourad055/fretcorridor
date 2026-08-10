package com.fretcorridor.gateway.domain;

/**
 * Représentation minimale d'un acteur authentifié, telle que nécessaire à la gateway
 * pour émettre un JWT. Le modèle complet (KYC, indice de conformité) appartient à
 * service-ida (Mobile) — la gateway n'en détient qu'un sous-ensemble de résolution.
 *
 * {@code delegationToken} : le JWT brut émis par service-ida lors de
 * l'authentification (null si l'authentification n'en a pas produit un, ex.
 * fixture de test). Le gateway signe son propre JWT pour ses routes (RG-002)
 * — mais service-exe/service-not/service-mkt (Mobile) valident les JWT
 * signés par service-ida, pas ceux du gateway. Sans ce token retransmis, le
 * gateway ne peut appeler aucun de ces services pour le compte de l'acteur
 * connecté. Cf. docs/adr (double autorité JWT).
 */
public record Actor(String actorId, String phone, Role role, String tenantId, String delegationToken) {
}
