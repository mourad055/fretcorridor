package com.fretcorridor.opt.client;

import java.time.Instant;

/**
 * Miroir cote OPT de PositionActuelleResponse (service-trk). Meme convention
 * que HubProcheDto/HubResponse (service-geo/service-opt) : pas de code
 * partage entre modules, chaque service possede son propre contrat
 * d'entree/sortie.
 */
public record PositionActuelleDto(double latitude, double longitude, Instant horodatageCapture) {
}
