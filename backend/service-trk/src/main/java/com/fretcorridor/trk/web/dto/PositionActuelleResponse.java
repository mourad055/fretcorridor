package com.fretcorridor.trk.web.dto;

import com.fretcorridor.trk.domain.Position;

import java.time.Instant;

/**
 * Reponse du GET /api/trk/positions/derniere (position temps reel d'un
 * vehicule, plan de reorientation post-demo). horodatageCapture expose
 * explicitement pour que l'appelant (OPT) puisse juger lui-meme de la
 * fraicheur de la donnee - jamais une decision prise silencieusement cote TRK.
 */
public record PositionActuelleResponse(double latitude, double longitude, Instant horodatageCapture) {
    public static PositionActuelleResponse from(Position position) {
        return new PositionActuelleResponse(
                position.getLatitude(), position.getLongitude(), position.getHorodatageCapture());
    }
}
