package com.fretcorridor.opt.messaging;

import java.util.UUID;

/**
 * Une etape (enlevement ou livraison) telle que publiee dans
 * TourneeConstitueeEvent. missionId = Affectation.id (meme convention que
 * AffectationConfirmeeEvent.missionId) - PAS un identifiant distinct.
 * fenetreDebut/fenetreFin toujours null a ce jour (Phase 1) : champs
 * presents dans le contrat pour anticiper CDC S13 (Demande.fenetre), non
 * encore modelises cote OPT.
 *
 * Contrat : shared-contracts/asyncapi/events/tournee-constituee.yaml
 */
public record EtapeConstitueeDto(
        UUID missionId,
        int rang,
        EtapeTypeDto typeEtape,
        UUID demandeId,
        double pointLatitude,
        double pointLongitude,
        java.time.Instant fenetreDebut,
        java.time.Instant fenetreFin
) {
    public enum EtapeTypeDto { ENLEVEMENT, LIVRAISON }
}
