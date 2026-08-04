package com.fretcorridor.opt.domain;

import com.fretcorridor.opt.client.CandidatCoutDto;

import java.util.List;
import java.util.UUID;

/**
 * Entree du L1 : une demande a apparier avec la liste de ses candidats
 * (capacites) deja filtres par L0 (GEO), chacun avec ses valeurs de critere
 * deja normalisees pour cette paire demande/candidat - meme convention que
 * CoutLotRequest cote service-mat.
 */
public record DemandeAvecCandidats(UUID demandeId, List<CandidatCoutDto> candidats) {
}
