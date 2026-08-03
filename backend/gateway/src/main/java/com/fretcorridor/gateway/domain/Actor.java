package com.fretcorridor.gateway.domain;

/**
 * Représentation minimale d'un acteur authentifié, telle que nécessaire à la gateway
 * pour émettre un JWT. Le modèle complet (KYC, indice de conformité) appartient à
 * service-ida (Mobile) — la gateway n'en détient qu'un sous-ensemble de résolution.
 */
public record Actor(String actorId, String phone, Role role, String tenantId) {
}
