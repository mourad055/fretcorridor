package com.fretcorridor.opt.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resultat d'affectation pour une demande donnee, apres L1 (Kuhn-Munkres).
 * capaciteId == null si aucune capacite n'a pu etre affectee ce cycle (plus
 * de demandes que de capacites disponibles dans le lot) - reprise au cycle
 * suivant plutot que forcee sur une capacite deja prise.
 */
public record AffectationResultat(UUID demandeId, UUID capaciteId, BigDecimal coutTotal, UUID cycleMatchingId) {
}
