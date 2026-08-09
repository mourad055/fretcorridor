package com.flysoft.fretcorridor.cap.messaging;

/**
 * Miroir local de com.fretcorridor.dto.PointGeoDto (common-libs). Duplique
 * plutot qu'importe : common-libs est compile cible Java 21, ce service
 * (comme le reste du perimetre Mobile, cf service-ida) tourne en Java 17 -
 * une dependance directe casserait la compilation (class file version
 * incompatible). Meme principe de duplication de contrat que
 * CandidatCoutDto cote service-opt.
 */
public record PointGeoDto(double latitude, double longitude) {
}
