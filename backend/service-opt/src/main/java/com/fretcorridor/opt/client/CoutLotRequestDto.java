package com.fretcorridor.opt.client;

import java.util.List;
import java.util.UUID;

// Miroir du contrat CoutLotRequest cote service-mat (RG-106 : axeId
// transmis pour resoudre le modele de ponderation specifique a l'axe,
// avec repli sur le modele par defaut cote MAT si absent/non trouve).
public record CoutLotRequestDto(UUID demandeId, UUID axeId, List<CandidatCoutDto> candidats) {
}
