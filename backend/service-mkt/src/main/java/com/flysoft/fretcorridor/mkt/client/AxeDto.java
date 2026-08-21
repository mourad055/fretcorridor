package com.flysoft.fretcorridor.mkt.client;

import java.util.UUID;

/**
 * Miroir partiel de AxeResponse cote service-geo - seuls les champs utiles a
 * la resolution d'axe par nom de ville sont repris ici (pas de code partage
 * entre modules, meme principe que ServiceFltClient/VehiculeDto cote
 * service-cap).
 */
public record AxeDto(
        UUID id, String hubOrigineVille, String hubDestinationVille,
        double hubOrigineLatitude, double hubOrigineLongitude,
        double hubDestinationLatitude, double hubDestinationLongitude
) {
}
