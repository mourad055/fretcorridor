package com.fretcorridor.opt.client;

import java.util.List;
import java.util.UUID;

public record CoutLotRequestDto(UUID demandeId, List<CandidatCoutDto> candidats) {
}
