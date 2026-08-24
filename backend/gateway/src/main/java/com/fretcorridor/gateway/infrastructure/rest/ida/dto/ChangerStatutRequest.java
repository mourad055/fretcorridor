package com.fretcorridor.gateway.infrastructure.rest.ida.dto;

import jakarta.validation.constraints.NotNull;

public record ChangerStatutRequest(@NotNull Boolean actif) {
}
