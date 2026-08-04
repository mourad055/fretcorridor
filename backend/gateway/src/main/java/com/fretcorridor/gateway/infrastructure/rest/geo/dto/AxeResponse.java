package com.fretcorridor.gateway.infrastructure.rest.geo.dto;

import com.fretcorridor.gateway.domain.geo.Axe;

public record AxeResponse(String id, String origine, String destination, double distanceKm, String etatActivation) {
    public static AxeResponse from(Axe axe) {
        return new AxeResponse(axe.id(), axe.origine(), axe.destination(), axe.distanceKm(), axe.etatActivation().name());
    }
}
