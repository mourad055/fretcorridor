package com.fretcorridor.opt.client;

import java.math.BigDecimal;
import java.util.UUID;

public record CoutResponseDto(UUID capaciteId, UUID cycleMatchingId, BigDecimal coutTotal) {
}
