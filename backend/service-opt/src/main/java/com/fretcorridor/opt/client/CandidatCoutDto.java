package com.fretcorridor.opt.client;

import java.util.Map;
import java.util.UUID;

/**
 * Miroir du contrat PaireCandidatRequest cote service-mat. Pas de code
 * partage entre modules (chaque microservice a son propre build, cf Plan
 * d'execution S4.1) - cette classe est intentionnellement dupliquee, pas
 * importee depuis service-mat.
 */
public record CandidatCoutDto(UUID capaciteId, Map<String, Double> valeursCriteres) {
}
