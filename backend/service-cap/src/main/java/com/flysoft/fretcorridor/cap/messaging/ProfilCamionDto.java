package com.flysoft.fretcorridor.cap.messaging;

/**
 * Miroir exact de com.fretcorridor.opt.client.ProfilCamionDto (service-opt) -
 * memes noms de champs, requis pour que la desserialisation Jackson cote
 * OPT fonctionne (matching par nom de propriete JSON, pas par type Java).
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
