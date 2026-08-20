package com.fretcorridor.pay.domain;

import java.time.Instant;

/**
 * EF-PAY-06, CDC §7.6 UC-PAY-01 étape 2 : moyen de paiement choisi par le
 * chargeur, distinct du moyen effectivement encaissé
 * ({@link EcritureMiroir#modePaiement()}). Le choix précède l'instruction
 * d'encaissement dans le flux nominal du CDC ; les deux faits restent
 * enregistrés séparément — ils pourraient diverger (nouvelle tentative
 * après échec d'encaissement) et rien n'impose qu'ils coïncident.
 *
 * Espèces (EF-PAY-07) hors de ce concept : mode dégradé décidé à
 * l'enlèvement, jamais choisi en amont dans l'application — cf.
 * {@link PaiementEspecesService}.
 */
public record ModePaiementChoisi(String missionId, String tenantId, ModePaiement modePaiement, Instant choisiLe) {
}
