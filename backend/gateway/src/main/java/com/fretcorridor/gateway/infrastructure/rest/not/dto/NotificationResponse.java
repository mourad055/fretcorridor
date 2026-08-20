package com.fretcorridor.gateway.infrastructure.rest.not.dto;

import com.fretcorridor.gateway.domain.not.Notification;

import java.time.Instant;

public record NotificationResponse(
        String id,
        String canal,
        String destinataire,
        String objet,
        String resume,
        Instant envoyeeLe
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.id(),
                notification.canal().name(),
                notification.destinataire(),
                notification.objet(),
                notification.resume(),
                notification.envoyeeLe()
        );
    }
}
