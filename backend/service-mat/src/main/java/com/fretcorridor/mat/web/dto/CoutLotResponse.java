package com.fretcorridor.mat.web.dto;

import java.util.List;
import java.util.UUID;

/**
 * Reponse du calcul de cout compose pour un lot entier : une seule version de
 * modele de ponderation et un seul indicateur mode_degrade pour tout le lot -
 * garantit que tous les candidats d'un meme cycle sont compares avec
 * exactement les memes poids, jamais un melange de versions en cours de lot.
 */
public record CoutLotResponse(
        UUID demandeId,
        Integer versionModeleUtilisee, // null si mode degrade (aucun modele actif trouve)
        boolean modeDegrade,
        List<CoutResponse> resultats
) {
}
