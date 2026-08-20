package com.fretcorridor.gateway.domain.adm;

import java.time.Instant;

public record ConfigurationVue(String cle, String perimetre, String valeur, String auteur, int version, Instant creeLe) {
}
