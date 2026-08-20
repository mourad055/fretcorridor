package com.fretcorridor.opt.messaging;

import java.util.Map;
import java.util.UUID;

/**
 * Un etat intermediaire du plan de chargement, tel que publie dans
 * PlanChargementConfirmeEvent (EF-MAT-13). Miroir de PlanChargement (entite
 * interne OPT), jamais l'entite elle-meme exposee directement - meme
 * separation contrat/persistance qu'ailleurs dans le moteur.
 *
 * chargesParEssieu : cle = identifiant d'essieu, valeur = charge en tonnes
 * a cet etat (JSONB cote OPT, transmis tel quel).
 */
public record EtatChargementDto(
        UUID etapeTourneeId,
        int rang,
        Map<String, Object> chargesParEssieu
) {
}
