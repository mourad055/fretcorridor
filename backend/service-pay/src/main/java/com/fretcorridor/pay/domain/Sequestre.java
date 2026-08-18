package com.fretcorridor.pay.domain;

import java.time.Instant;

/**
 * État logique reflétant le cantonnement chez le prestataire (CDC §13) —
 * 1-1 Mission. {@code tenantId}/{@code transporteurId} sont {@code null}
 * tant que {@code DECLENCHE} — connus seulement à la libération (clôture),
 * cf. ADR 0015 : {@code libereLe} sert de point de départ au délai de
 * contestation pour l'ordonnanceur de reversement automatique (EF-PAY-08).
 *
 * RG-078 : un séquestre {@code LIBERE} porte toujours une preuve de
 * livraison — garanti par construction, pas par convention à l'appel. La
 * preuve elle-même (sa nature, son authenticité) appartient au domaine
 * Mission/EXE, hors périmètre ; ce champ n'en est qu'une référence tracée.
 */
public record Sequestre(String missionId, SequestreEtat etat, Instant declencheLe, Instant libereLe,
                         String tenantId, String transporteurId, String preuveLivraisonReference) {
    public Sequestre {
        if (etat == SequestreEtat.LIBERE && (preuveLivraisonReference == null || preuveLivraisonReference.isBlank())) {
            throw new IllegalArgumentException("RG-078 : un séquestre libéré doit porter une preuve de livraison enregistrée");
        }
    }
}
