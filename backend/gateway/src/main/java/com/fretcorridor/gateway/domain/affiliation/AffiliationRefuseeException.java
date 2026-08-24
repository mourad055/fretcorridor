package com.fretcorridor.gateway.domain.affiliation;

/**
 * S18 : invitation refusée par service-ida (numéro inconnu, acteur non
 * transporteur/chauffeur) — jamais confondue avec une indisponibilité de
 * service (audit UX 2026-08-24 : RealAffiliationAdapter.inviter() mappait
 * jusqu'ici toute erreur, y compris ce refus métier 400, sur
 * AffiliationServiceIndisponibleException — un Bureau qui invitait un
 * mauvais numéro voyait "service indisponible" au lieu du vrai motif).
 */
public class AffiliationRefuseeException extends RuntimeException {
    public AffiliationRefuseeException(String message) {
        super(message);
    }
}
