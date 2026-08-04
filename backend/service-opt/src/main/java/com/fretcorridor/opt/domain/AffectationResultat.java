package com.fretcorridor.opt.domain;

import com.fretcorridor.opt.client.ItineraireResponseDto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resultat d'affectation pour une demande donnee, apres L1 (Kuhn-Munkres).
 * capaciteId == null si aucune capacite n'a pu etre affectee ce cycle (plus
 * de demandes que de capacites disponibles dans le lot) - reprise au cycle
 * suivant plutot que forcee sur une capacite deja prise.
 *
 * itineraire == null dans deux cas bien distincts, jamais confondus dans les
 * logs (cf AffectationL1Service) :
 *   1) capaciteId == null : pas d'affectation, donc pas d'itineraire a
 *      calculer - cas normal, pas un mode degrade.
 *   2) capaciteId != null mais Valhalla injoignable/en echec (ENF-DIS-04) :
 *      affectation valide, itineraire degrade - l'ETA/tarification en aval
 *      doivent gerer explicitement ce cas, jamais improviser une distance a
 *      vol d'oiseau en remplacement silencieux (cf javadoc ValhallaClient).
 */
public record AffectationResultat(
        UUID demandeId,
        UUID capaciteId,
        BigDecimal coutTotal,
        UUID cycleMatchingId,
        ItineraireResponseDto itineraire,
        // Etape 4 du moteur V0 (README, "Tarification en sortie de cycle").
        // Meme convention que itineraire : null si pas d'affectation (cas
        // normal), TarificationResultat.modeDegrade()==true si affectation
        // valide mais prix non calculable (cf TarificationL4Service) -
        // jamais confondus, jamais de prix invente en silence.
        com.fretcorridor.opt.tarification.TarificationResultat tarification
) {
}
