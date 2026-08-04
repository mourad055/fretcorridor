package com.fretcorridor.opt.client;

/**
 * Profil camion transmis a Valhalla pour un calcul d'itineraire compatible
 * poids-lourd (evite les routes interdites aux camions, ponts sous-dimensionnes,
 * etc.) - champs cites explicitement au CDC S8.11.2 : hauteur, largeur, longueur,
 * poids max, charge max par essieu, nombre d'essieux.
 *
 * ATTENTION (CDC S8.11.2) : la completude de ces attributs dans les donnees
 * ouvertes africaines est faible et non documentee - a traiter comme source
 * d'incertitude structurelle, pas comme donnee acquise. Les champs restent
 * nullable ici pour cette raison : un profil partiellement renseigne doit
 * degrader le calcul Valhalla (routage moins precis), jamais le faire echouer.
 */
public record ProfilCamionDto(
        Double hauteurMetres,
        Double largeurMetres,
        Double longueurMetres,
        Double poidsMaxTonnes,
        Double chargeMaxParEssieuTonnes,
        Integer nombreEssieux,
        boolean matieresDangereuses
) {
}
