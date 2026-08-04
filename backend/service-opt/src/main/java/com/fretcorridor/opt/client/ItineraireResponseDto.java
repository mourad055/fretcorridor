package com.fretcorridor.opt.client;

/**
 * Itineraire retenu par Valhalla - distance et duree servent au calcul d'ETA
 * (consomme ensuite par TRK, cf Plan d'execution S4.2 "service-opt -> service-trk :
 * itineraire retenu, pour le calcul d'ETA").
 *
 * intervalleConfianceSecondes : marge appliquee sur la duree brute Valhalla,
 * necessaire pour ENF-TRK-01/02 ("ETA dynamique avec intervalle de confiance")
 * - calculee ici plutot que devinee cote TRK, puisque OPT est le seul a
 * connaitre a la fois l'itineraire brut et le profil camion utilise.
 */
public record ItineraireResponseDto(
        double distanceMetres,
        double dureeSecondes,
        double intervalleConfianceSecondes,
        String geometrieEncodee
) {
}
