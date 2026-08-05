package com.fretcorridor.gateway.domain.not;

import java.time.Instant;

/**
 * Vue minimale d'une notification envoyée (Sprint 9). Le modèle complet
 * (multicanal, discipline de coût) appartient à service-not (Mobile).
 */
public record Notification(
        String id,
        String tenantId,
        CanalNotification canal,
        String destinataire,
        String objet,
        String resume,
        Instant envoyeeLe
) {
}
