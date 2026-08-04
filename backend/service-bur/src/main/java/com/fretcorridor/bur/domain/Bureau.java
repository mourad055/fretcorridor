package com.fretcorridor.bur.domain;

/**
 * Institution supervisant un territoire (CDC §13, modèle de données). 1-1 avec un
 * Tenant — c'est le rattachement qui porte l'isolation multi-tenant (ENF-MUL-01).
 */
public record Bureau(String id, String tenantId, String nom) {

    public Bureau {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("L'identifiant du bureau est obligatoire");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Un bureau doit être rattaché à un tenant");
        }
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom du bureau est obligatoire");
        }
    }
}
