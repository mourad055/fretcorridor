package com.fretcorridor.gateway.infrastructure.rest.adm.dto;

import com.fretcorridor.gateway.domain.adm.ConfigurationVue;

import java.time.Instant;

public record ConfigurationResponse(String cle, String perimetre, String valeur, String auteur, int version, Instant creeLe) {
    public static ConfigurationResponse from(ConfigurationVue configuration) {
        return new ConfigurationResponse(configuration.cle(), configuration.perimetre(), configuration.valeur(),
                configuration.auteur(), configuration.version(), configuration.creeLe());
    }
}
