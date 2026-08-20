package com.fretcorridor.adm.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record EnregistrerAuditRequest(
        String tenantId,
        @NotBlank(message = "L'acteur est obligatoire") String acteurId,
        @NotBlank(message = "L'action est obligatoire") String action,
        @NotBlank(message = "La ressource est obligatoire") String ressource
) {
}
