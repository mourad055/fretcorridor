package com.fretcorridor.gateway.domain.cap;

import java.math.BigDecimal;

public record ConfigurerTarifChauffeur(String axeId, String categorieMarchandise, BigDecimal tarifFcfaParKg) {
}
