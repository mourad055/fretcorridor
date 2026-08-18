package com.flysoft.fretcorridor.not.client;

import java.util.UUID;

/**
 * Copie locale minimale de CapaciteResponse (service-cap) — seul
 * transporteurId nous intéresse ici (résolution du destinataire de la
 * notification de retour à vide), pas de bibliothèque Java partagée entre
 * porteurs (même principe que les autres miroirs d'événements du dépôt).
 */
public record CapaciteDto(UUID id, UUID transporteurId) {
}
