package com.fretcorridor.gateway.domain;

/**
 * Rôles du triplet acteur × rôle × périmètre (CDC v4.0 §5.2, RG-002).
 * Aucun rôle "Admin" par défaut : toute élévation passe par une décision explicite,
 * jamais par un rôle codé en dur côté client (garde-fou G4).
 *
 * CHAUFFEUR/CHAUFFEUR_PROPRIETAIRE/AGENT/CHARGEUR : rôles mobiles (app Client
 * et app Chauffeur/Transporteur), ajoutés pour que ServiceIdaAuthenticationAdapter
 * puisse authentifier ces acteurs — jusqu'ici seuls BUREAU/TRANSPORTEUR/ADMIN
 * (rôles Web) étaient reconnus, ce qui rejetait tout login mobile en
 * InvalidCredentialsException malgré des identifiants valides côté service-ida.
 */
public enum Role {
    BUREAU,
    TRANSPORTEUR,
    ADMIN,
    CHAUFFEUR,
    CHAUFFEUR_PROPRIETAIRE,
    AGENT,
    CHARGEUR
}
