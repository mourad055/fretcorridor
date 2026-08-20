package com.fretcorridor.gateway.infrastructure.rest.pay.dto;

import java.math.BigDecimal;
import java.util.List;

public record SoldeTransporteurResponse(BigDecimal solde, List<EcritureVueResponse> historique) {
}
