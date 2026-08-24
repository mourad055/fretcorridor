package com.fretcorridor.gateway.infrastructure.rest.ida.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record ChangerRolesRequest(@NotEmpty Set<String> roles) {
}
