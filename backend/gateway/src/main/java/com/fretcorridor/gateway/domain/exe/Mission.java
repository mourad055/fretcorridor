package com.fretcorridor.gateway.domain.exe;

import java.util.List;

/**
 * Vue minimale d'une mission avec sa chronologie d'étapes (Sprint 7). Le
 * modèle complet (preuves, empreinte cryptographique) appartient à
 * service-exe (Mobile, CDC UC-EXE, EF-EXE-01 à 07).
 */
public record Mission(
        String id,
        String tenantId,
        String transporteurId,
        String transporteurNom,
        String origine,
        String destination,
        List<EtapeMission> etapes
) {
}
