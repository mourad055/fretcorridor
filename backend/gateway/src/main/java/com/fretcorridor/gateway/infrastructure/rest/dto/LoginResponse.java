package com.fretcorridor.gateway.infrastructure.rest.dto;

public record LoginResponse(String token, String role, String tenantId) {
}
