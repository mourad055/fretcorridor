package com.fretcorridor.opt.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * EF-MAT-13 (CDC S8.7, priorite S) : "plan de chargement exploitable
 * restitue au chauffeur." Publie uniquement pour une Tournee CONFIRMEE
 * (jamais pour un rejet oracle - E1, cf OracleChargementService) - une
 * Tournee non confirmee n'a rien a restituer, l'app ne doit jamais
 * recevoir un plan pour une consolidation refusee.
 *
 * Evenement DEDIE, separe de TourneeConstitueeEvent (S11, ordre des
 * etapes) : le detail essieu par essieu repond a un besoin different
 * (S16, priorite S) - meme principe de separation que
 * demande-publiee-lots.yaml vs demande-publiee.yaml, pour ne pas
 * alourdir un contrat deja valide avec Personne 1 pour un autre usage.
 *
 * BROUILLON - contrat non encore valide avec Personne 1 (Mobile,
 * app Chauffeur/Transporteur, ecran "restitution du plan de chargement"
 * mentionne au Sprint 16 du Plan d'execution).
 */
public record PlanChargementConfirmeEvent(
        UUID eventId,
        UUID tourneeId,
        List<EtatChargementDto> etats,
        Instant dateGeneration
) {
}
