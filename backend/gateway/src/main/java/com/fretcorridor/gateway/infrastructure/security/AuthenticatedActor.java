package com.fretcorridor.gateway.infrastructure.security;

import com.fretcorridor.gateway.domain.Role;

/**
 * Principal résolu depuis le JWT — porte le triplet acteur × rôle × tenant (RG-002).
 * Toute donnée restituée par un contrôleur doit être filtrée par tenantId, jamais
 * par un paramètre de requête modifiable par le client (ENF-MUL-01).
 */
public record AuthenticatedActor(String actorId, Role role, String tenantId) {
}
