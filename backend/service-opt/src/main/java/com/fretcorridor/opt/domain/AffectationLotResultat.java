package com.fretcorridor.opt.domain;

import java.util.List;

/**
 * Resultat global d'un cycle d'affectation L1.
 * modeDegrade=true si service-mat etait injoignable - dans ce cas
 * affectations est vide : on ne bloque jamais (ENF-DIS-04) mais on n'affecte
 * pas non plus sans cout fiable, pour ne pas violer EF-MAT-01 (jamais
 * glouton) en improvisant un critere de remplacement.
 */
public record AffectationLotResultat(boolean modeDegrade, List<AffectationResultat> affectations) {
}
