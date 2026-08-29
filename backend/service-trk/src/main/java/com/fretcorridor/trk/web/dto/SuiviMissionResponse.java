package com.fretcorridor.trk.web.dto;

import java.time.Instant;

/**
 * Sortie du GET /api/trk/suivi/{missionId} (point 6 du plan de reorientation :
 * "colis recupere = position chauffeur").
 *
 * Bascule l'affichage du suivi : tant que le colis n'est pas recupere
 * (enlevement non execute), la position a afficher est la position ESTIMEE du
 * colis (son point d'enlevement, porte par l'affectation cote OPT) ; des que
 * l'enlevement est confirme, la position a afficher est la position GPS temps
 * reel du chauffeur (derniere capture TRK).
 *
 * positionAffichee indique explicitement laquelle des deux sources choisir -
 * jamais une decision silencieuse cote TRK (meme philosophie que
 * PositionActuelleResponse.horodatageCapture : l'appelant juge, TRK expose).
 */
public record SuiviMissionResponse(
        boolean colisRecupere,
        SourcePosition sourcePosition,
        double latitude,
        double longitude,
        Instant horodatagePosition,
        Instant horodatageEnlevement
) {
    public enum SourcePosition {
        GPS_CHAUFFEUR, // colis a bord : passee par le vehicule (position temps reel)
        POSITION_ESTIMEE // colis pas encore recupere : son point d'enlevement
    }
}
