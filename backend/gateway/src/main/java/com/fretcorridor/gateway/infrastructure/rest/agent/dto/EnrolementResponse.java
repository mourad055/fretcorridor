package com.fretcorridor.gateway.infrastructure.rest.agent.dto;

import com.fretcorridor.gateway.domain.agent.Enrolement;

public record EnrolementResponse(String enrolementId, String telephone, String typeActeur, String statut) {
    public static EnrolementResponse from(Enrolement e) {
        return new EnrolementResponse(e.enrolementId(), e.telephone(), e.typeActeur(), e.statut());
    }
}
