package com.fretcorridor.gateway.domain.agent;

/** UC-IDA-03 : enrôlement assisté par agent (EF-IDA-06). */
public record Enrolement(String enrolementId, String telephone, String typeActeur, String statut) {
}
