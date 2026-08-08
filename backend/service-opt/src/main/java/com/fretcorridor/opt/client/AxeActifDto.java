package com.fretcorridor.opt.client;

import java.util.Map;
import java.util.UUID;

/**
 * Miroir partiel du contrat AxeResponse cote service-geo - seuls les champs
 * utiles a OPT/MAT sont repris ici (pas de code partage entre modules, cf
 * Plan d'execution S4.1).
 *
 * parametres (JSONB, EF-GEO-02) porte notamment "rayonMatchingKm" - lu par
 * MatchingCycleService pour respecter EF-MAT-01 ("rayon d'appariement
 * borne par axe"). Cle absente = pas de borne appliquee ce cycle (a
 * documenter comme limitation, pas une valeur par defaut inventee ici).
 */
public record AxeActifDto(UUID id, String nom, Map<String, Object> parametres) {
}
