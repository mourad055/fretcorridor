package com.fretcorridor.trk.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Corps de requete pour l'appel groupe (plan de reorientation, position GPS
 * temps reel dans le matching). POST plutot que GET+query multiple : une
 * liste de vehiculeIds candidats par cycle peut depasser confortablement la
 * limite pratique d'une query string.
 */
public record PositionsBatchRequest(
        @NotEmpty
        @Size(max = 200, message = "200 vehicules maximum par appel groupe")
        List<UUID> vehiculeIds
) {
}
