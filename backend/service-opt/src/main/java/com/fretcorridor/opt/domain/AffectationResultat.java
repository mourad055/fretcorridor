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
 * missionId : identifiant genere UNIQUEMENT quand capaciteId != null (cf
 * AffectationL1Service.persisterSiAffecte) - c'est l'id de la ligne
 * opt.affectation correspondante. Point d'ancrage du contrat comble ici :
 * TRK appellera GET /api/opt/affectations/{missionId} en synchrone interne
 * pour recuperer origine/destination et calculer son ETA ; le meme id sera
 * porte par l'evenement Kafka AffectationConfirmee vers service-exe (Mobile)
 * quand ce producteur sera code, pour que Mobile et TRK parlent de la meme
 * mission sans ambiguite.
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
        UUID missionId,
        BigDecimal coutTotal,
        UUID cycleMatchingId,
        ItineraireResponseDto itineraire,
        com.fretcorridor.opt.tarification.TarificationResultat tarification
) {
}
