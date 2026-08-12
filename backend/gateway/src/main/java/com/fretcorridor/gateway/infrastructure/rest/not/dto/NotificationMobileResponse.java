package com.fretcorridor.gateway.infrastructure.rest.not.dto;

import com.fretcorridor.gateway.domain.not.NotificationMobile;

public record NotificationMobileResponse(String id, String titre, String corps, String type, String referenceId,
                                          boolean lue, String dateCreation) {
    public static NotificationMobileResponse from(NotificationMobile n) {
        return new NotificationMobileResponse(n.id(), n.titre(), n.corps(), n.type(), n.referenceId(), n.lue(), n.dateCreation());
    }
}
