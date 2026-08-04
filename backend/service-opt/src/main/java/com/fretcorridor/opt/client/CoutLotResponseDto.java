package com.fretcorridor.opt.client;

import java.util.List;
import java.util.UUID;

public record CoutLotResponseDto(UUID demandeId, Integer versionModeleUtilisee,
                                   boolean modeDegrade, List<CoutResponseDto> resultats) {
}
