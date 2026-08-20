package com.flysoft.fretcorridor.flt.client;

import java.util.UUID;

/**
 * Miroir partiel de ChronologieResponse cote service-exe - seul le champ
 * utile a la publication de PositionBrute est repris ici (pas de code
 * partage entre modules, meme principe que ServiceFltClient/VehiculeDto
 * cote service-cap).
 */
public record MissionDto(UUID vehiculeId) {
}
