package com.fretcorridor.gateway.domain.pay;

import java.time.Instant;

/**
 * S14 Item B (EF-PAY-06/07) : moyen de paiement choisi par le chargeur à
 * l'instruction d'encaissement — distinct du moyen effectivement encaissé
 * (voir {@link EcritureVue#modePaiement()}). Lecture seule côté gateway,
 * réservée à l'affichage Chauffeur (S14 Volet A) ; le choix lui-même (S14
 * Volet B, Client) se fait en appelant `service-pay` directement, l'app
 * Client n'ayant pas de gateway unifiée à ce jour.
 */
public record ModePaiementChoisi(String missionId, String modePaiement, Instant choisiLe) {
}
