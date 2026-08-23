package com.fretcorridor.gateway.infrastructure.security;

import com.fretcorridor.gateway.domain.Role;

/**
 * Principal résolu depuis le JWT — porte le triplet acteur × rôle × tenant (RG-002).
 * Toute donnée restituée par un contrôleur doit être filtrée par tenantId, jamais
 * par un paramètre de requête modifiable par le client (ENF-MUL-01).
 *
 * {@code delegationToken} : token service-ida retransmis (peut être null —
 * absent si l'acteur ne s'est pas authentifié via service-ida, ex. anciens
 * flux de test). À utiliser en {@code Authorization: Bearer} pour tout appel
 * du gateway vers un service Mobile qui valide les JWT service-ida
 * (service-exe/service-not/service-mkt) — jamais le JWT du gateway lui-même,
 * qu'ils ne reconnaissent pas.
 */
public record AuthenticatedActor(String actorId, Role role, String tenantId, String delegationToken, String phone) {
}
