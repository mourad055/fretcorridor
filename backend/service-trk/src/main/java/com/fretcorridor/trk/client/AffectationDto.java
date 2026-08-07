package com.fretcorridor.trk.client;

import java.util.UUID;

/**
 * Miroir partiel du contrat AffectationResponse cote service-opt - seuls les
 * champs necessaires au calcul d'ETA sont repris ici (pas de code partage
 * entre modules, cf Plan d'execution S4.1). Comble le trou d'architecture :
 * avant ce client, TRK n'avait aucun moyen de connaitre la destination
 * reelle d'une mission (cf AffectationController, cote service-opt).
 */
public record AffectationDto(
        UUID missionId,
        double origineLatitude,
        double origineLongitude,
        double destinationLatitude,
        double destinationLongitude
) {
}
