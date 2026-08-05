package com.fretcorridor.opt.client;

import com.fretcorridor.dto.PointGeoDto;

import java.util.List;

/**
 * Requete d'itineraire vers Valhalla : suite ordonnee de points (origine,
 * eventuelles etapes intermediaires, destination) + profil camion.
 */
public record ItineraireRequestDto(List<PointGeoDto> points, ProfilCamionDto profilCamion) {
}
