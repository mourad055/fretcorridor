package com.flysoft.fretcorridor.mkt.client;

import java.util.UUID;

/**
 * Miroir partiel de AxeResponse cote service-geo - seuls les champs utiles a
 * la resolution d'axe par nom de ville sont repris ici (pas de code partage
 * entre modules, meme principe que ServiceFltClient/VehiculeDto cote
 * service-cap).
 *
 * parametres (JSONB, EF-GEO-02) : ajoute pour RG-101 (coefficient
 * volumetrique resolu par axe, jamais une seule valeur globale) - meme
 * cle/pattern que AxeActifDto cote service-opt et CalculateurPoidsTaxable
 * cote service-cap.
 */
public record AxeDto(
        UUID id, String hubOrigineVille, String hubDestinationVille,
        double hubOrigineLatitude, double hubOrigineLongitude,
        double hubDestinationLatitude, double hubDestinationLongitude,
        java.util.Map<String, Object> parametres
) {
}
