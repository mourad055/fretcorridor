package com.fretcorridor.bur.domain;

import java.math.BigDecimal;

/**
 * {@code evaluable=false} quand l'indicateur sous-jacent est masqué par le
 * seuil d'agrégation (EF-BUR-04/RG-085) — jamais déclenchée dans ce cas,
 * une alerte ne doit jamais se baser sur une donnée ré-identifiante non
 * restituée par ailleurs.
 */
public record EtatAlerte(AlerteSeuil alerte, boolean evaluable, boolean declenchee, BigDecimal valeurActuelle) {
}
