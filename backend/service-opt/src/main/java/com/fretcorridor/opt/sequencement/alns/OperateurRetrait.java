package com.fretcorridor.opt.sequencement.alns;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Operateur de destruction ALNS (CDC S8.6.2). V1 : retrait aleatoire simple
 * (un ou plusieurs affectationId retires de la sequence). Interface volontai-
 * rement minimale pour permettre d'ajouter d'autres strategies (retrait par
 * proximite geographique, retrait du plus couteux) sans toucher AlnsSolver -
 * l'"adaptive" de ALNS suppose plusieurs operateurs parmi lesquels choisir,
 * ce premier operateur est le point de depart, pas la version finale visee.
 */
public class OperateurRetrait {

    private final Random random;

    public OperateurRetrait(Random random) {
        this.random = random;
    }

    /** Retire aleatoirement nbARetirer affectations de la liste fournie. */
    public List<UUID> selectionnerPourRetrait(List<UUID> affectationIdsDansLaSolution, int nbARetirer) {
        List<UUID> copie = new java.util.ArrayList<>(affectationIdsDansLaSolution);
        java.util.Collections.shuffle(copie, random);
        return copie.stream().limit(Math.min(nbARetirer, copie.size())).toList();
    }
}
