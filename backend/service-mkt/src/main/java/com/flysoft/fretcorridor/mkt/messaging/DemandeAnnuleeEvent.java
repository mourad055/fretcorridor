package com.flysoft.fretcorridor.mkt.messaging;

import java.util.UUID;

/**
 * Publie quand une demande est annulee (DemandeService.annuler) - permet a
 * service-opt de retirer la demande de sa file d'attente de matching
 * (opt.demande_en_attente) avant qu'un cycle ne gaspille une capacite dessus.
 *
 * BUG CORRIGE (retour utilisateur direct, 22 aout) : annuler() ne
 * previenait jusqu'ici jamais le Moteur - une demande annulee restait
 * "en attente" cote service-opt et pouvait etre matchee normalement,
 * consommant une capacite reelle pour rien.
 */
public record DemandeAnnuleeEvent(UUID eventId, UUID demandeId) {
}
