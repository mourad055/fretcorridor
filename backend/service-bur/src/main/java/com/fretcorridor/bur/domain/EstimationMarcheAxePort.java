package com.fretcorridor.bur.domain;

import java.util.Optional;
import java.util.UUID;

public interface EstimationMarcheAxePort {

    /** Remplace l'estimation active de l'axe si une existait déjà (une seule par axe). */
    void definir(EstimationMarcheAxe estimation);

    Optional<EstimationMarcheAxe> pour(String tenantId, UUID axeId);
}
