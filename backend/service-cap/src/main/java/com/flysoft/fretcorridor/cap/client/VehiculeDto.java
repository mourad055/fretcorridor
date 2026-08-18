package com.flysoft.fretcorridor.cap.client;

import java.util.UUID;

/**
 * Miroir partiel de VehiculeResponse cote service-flt - seul le champ utile
 * a la resolution du transporteur est repris ici (pas de code partage entre
 * modules, meme principe que le reste du monorepo).
 */
public record VehiculeDto(UUID id, UUID proprietaireActeurId) {
}
