package com.fretcorridor.gateway.domain;

/**
 * Rôles du triplet acteur × rôle × périmètre (CDC v4.0 §5.2, RG-002).
 * Aucun rôle "Admin" par défaut : toute élévation passe par une décision explicite,
 * jamais par un rôle codé en dur côté client (garde-fou G4).
 */
public enum Role {
    BUREAU,
    TRANSPORTEUR,
    ADMIN
}
