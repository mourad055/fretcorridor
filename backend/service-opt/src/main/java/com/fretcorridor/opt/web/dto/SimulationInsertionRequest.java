package com.fretcorridor.opt.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entree de POST /api/opt/simulation-insertion (plan de reorientation,
 * point 4) : une nouvelle demande a inserer dans la tournee EN COURS du
 * chauffeur, represente par sa capacite (capaciteId). L'origin/la
 * destination du colis correspondent respectivement au point d'enlevement et
 * au point de livraison ; le poids sert a la contrainte de capacite dynamique.
 *
 * AUCUNE donnee de ce corps n'est persistee : la simulation est un dry-run
 * completement in-memory (cf SimulationInsertionService).
 */
public record SimulationInsertionRequest(
        UUID capaciteId,
        UUID axeId,
        double origineLatitude,
        double origineLongitude,
        double destinationLatitude,
        double destinationLongitude,
        BigDecimal poidsKg,
        boolean matieresDangereuses
) {
}
