package com.fretcorridor.opt.client;

import java.util.Map;
import java.util.UUID;

/**
 * Miroir partiel du contrat AxeResponse cote service-geo (GET /api/geo/axes/{id})
 * - seuls les champs utiles a OPT sont repris ici (pas de code partage entre
 * modules, cf Plan d'execution S4.1). Utilise pour lire
 * parametres.conventionRepartition (EF-GEO-05/RG-052, Phase 4).
 */
public record AxeDetailDto(UUID id, Map<String, Object> parametres) {
}
