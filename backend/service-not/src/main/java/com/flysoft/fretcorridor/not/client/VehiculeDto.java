package com.flysoft.fretcorridor.not.client;

import java.util.UUID;

/**
 * Miroir partiel de VehiculeResponse cote service-flt - seul le champ utile
 * a la resolution du transporteur pour AlerteEcart est repris ici (meme
 * principe que le ServiceFltClient de service-cap).
 */
public record VehiculeDto(UUID id, UUID proprietaireActeurId) {
}
