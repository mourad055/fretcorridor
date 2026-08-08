package com.fretcorridor.mat.domain;

import java.util.Map;
import java.util.UUID;

/**
 * Entree de calcul cote metier, independante du DTO web (CoutLotRequest /
 * PaireCandidatRequest) - evite de coupler la logique de calcul au contrat
 * HTTP expose. Meme separation que HubProcheDto/HubResponse cote
 * service-geo/service-opt.
 */
public record CandidatCout(UUID capaciteId, Map<String, Double> valeursCriteres) {
}
