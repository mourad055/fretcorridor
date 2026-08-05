package com.fretcorridor.adm.domain;

import java.time.Instant;

/**
 * Paramètre versionné (FE-ADM-03) : jamais modifié en place, chaque
 * changement crée une nouvelle version horodatée et auditée (anti-patron
 * CDC §12.4 — pas de configuration en fichier pour une règle métier).
 */
public record ConfigurationVersionnee(
        String id,
        String cle,
        String perimetre,
        String valeur,
        String auteur,
        int version,
        Instant creeLe
) {
    public static final String PERIMETRE_GLOBAL = "GLOBAL";
}
