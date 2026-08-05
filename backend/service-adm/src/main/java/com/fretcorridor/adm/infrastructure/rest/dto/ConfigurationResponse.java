package com.fretcorridor.adm.infrastructure.rest.dto;

import com.fretcorridor.adm.domain.ConfigurationVersionnee;

import java.time.Instant;

public record ConfigurationResponse(
        String cle,
        String perimetre,
        String valeur,
        String auteur,
        int version,
        Instant creeLe
) {
    public static ConfigurationResponse from(ConfigurationVersionnee configuration) {
        return new ConfigurationResponse(configuration.cle(), configuration.perimetre(), configuration.valeur(),
                configuration.auteur(), configuration.version(), configuration.creeLe());
    }
}
