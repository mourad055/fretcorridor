package com.fretcorridor.opt.messaging;

import com.fretcorridor.dto.PointGeoDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Payload Kafka de l'evenement DemandePubliee (service-mkt, Mobile -> OPT/MAT).
 * BROUILLON - contrat non encore valide avec Personne 1 (Mobile, service-mkt).
 *
 * fenetreDebut/fenetreFin (Sprint 12) : nullable - absent tant que Mobile ne
 * les publie pas (cf shared-contracts/asyncapi/events/demande-publiee.yaml).
 * Debloque le terme "retard" de CoutSolution cote donnee ; le calcul du
 * retard lui-meme reste un TODO separe tant qu'AlnsSolver n'invoque pas
 * CoutSolution.evaluer() (V1 actuelle : cout marginal = distance, cf
 * OperateurInsertion javadoc "V1 : cout marginal = distance ajoutee").
 */
public record DemandePublieeEvent(
        UUID eventId,
        UUID demandeId,
        UUID axeId,
        Map<String, Double> valeursCriteres,
        PointGeoDto origine,
        PointGeoDto destination,
        BigDecimal poidsTaxableKg,
        Instant fenetreDebut,
        Instant fenetreFin,
        // Infos marchandise (audit de suivi Mobile) : desormais publiees par
        // service-mkt, propagees jusqu'a Mission (service-exe) pour que
        // l'app Chauffeur sache ce qu'elle transporte.
        String typeEmballageNom,
        Integer quantite
) {
}
