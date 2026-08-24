package com.fretcorridor.gateway.domain.ida;

import java.util.Set;

/** Gestion des comptes par un Admin (audit UX 2026-08-23, §1.1) — appelle service-ida (Mobile), source d'identité unique. */
public record CompteAdmin(
        String id,
        String telephone,
        String nom,
        String prenom,
        String raisonSociale,
        String tenantId,
        Set<String> roles,
        boolean actif,
        String niveauKyc
) {
}
