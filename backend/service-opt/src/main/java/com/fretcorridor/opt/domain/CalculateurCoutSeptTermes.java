package com.fretcorridor.opt.domain;

import com.fretcorridor.dto.PointGeoDto;
import com.fretcorridor.util.HaversineUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calcul des 7 termes du cout composite (CDC S8.5.3) pour UNE paire
 * demande/candidat :
 *
 *   COUT = a1 x KM_APPROCHE      + a2 x KM_DETOUR       + a3 x ECART_TEMPOREL
 *        - a4 x GAIN_REMPLISSAGE - a5 x FIABILITE       + a6 x RISQUE_AXE
 *        - a7 x VALEUR_RETOUR
 *
 * Les poids a1..a7 vivent dans mat.modele_ponderation (RG-106 : configures
 * par axe, versionnes - cf V4__modele_ponderation_sept_termes_cdc.sql).
 * Les termes favorables du CDC (signe "-") sont encodes par un POIDS NEGATIF
 * dans le modele, car le moteur MAT est une somme ponderee purement additive :
 * la convention de signe est donc porteuse par les donnees, pas par le code.
 *
 * Cette classe ne DECIDE rien et ne PONDERE rien : elle mesure. La decision
 * reste cote AffectationL1Service (Kuhn-Munkres), la ponderation cote MAT.
 *
 * Hypotheses documentees (meme statut que risque SURVEILLE -> 0.5, assumees
 * et a valider en equipe, jamais presentees comme des valeurs du CDC) :
 *  - KM_DETOUR vaut 0.0 au stade L1 : "allongement impose a une tournee
 *    existante" n'a pas de sens avant le sequencement (L2/ALNS evalue deja
 *    l'insertion marginale). Terme neutre ici, reel apres insertion.
 *  - FIABILITE est relai de la valeur publiee par service-cap
 *    (historique d'execution hors perimetre cap V0, placeholder 0.5 neutre).
 *  - VALEUR_RETOUR = densite de fret en attente autour de la destination :
 *    nombre d'AUTRES demandes dont l'origine est a moins de
 *    fretcorridor.opt.matching.valeur-retour-rayon-km de la destination de
 *    cette demande, sature a fretcorridor.opt.matching.valeur-retour-saturation
 *    demandes (= score 1.0). Proxy mesurable du "fret au retour" prospectif,
 *    sans modele appris (CDC S8.5.3 place l'apprentissage au palier V3).
 */
@Component
public class CalculateurCoutSeptTermes {

    // Placeholder neutre si service-cap n'a pas publie de FIABILITE connue -
    // meme valeur que le placeholder cap, pour ne pas biaiser la comparaison
    // entre candidats qui l'ont et ceux qui ne l'ont pas.
    static final double FIABILITE_DEFAUT_NEUTRE = 0.5;

    private final double valeurRetourRayonKm;
    private final double valeurRetourSaturation;

    public CalculateurCoutSeptTermes(
            @Value("${fretcorridor.opt.matching.valeur-retour-rayon-km:50}") double valeurRetourRayonKm,
            @Value("${fretcorridor.opt.matching.valeur-retour-saturation:5}") double valeurRetourSaturation) {
        this.valeurRetourRayonKm = valeurRetourRayonKm;
        this.valeurRetourSaturation = valeurRetourSaturation;
    }

    /**
     * Les 7 criteres CDC pour la paire (demande, capacite), prets a etre
     * ponderes par service-mat avec le modele actif de l'axe (RG-106).
     *
     * @param nbAutresDemandesProchesDestination nombre d'autres demandes en
     *        attente dont l'origine est proche de la destination de cette
     *        demande (calcule une fois par cycle par l'appelant - donnee de
     *        contexte collective, pas propriete de la paire)
     * @param risqueAxeScore score [0,1] issu d'Axe.parametres.risqueSecuritaire
     *        (0.0 NORMAL, 0.5 SURVEILLE - cf extraireRisqueAxeScore dans
     *        MatchingCycleService)
     */
    public Map<String, Double> calculer(DemandeEnAttente demande,
                                         CapaciteEnAttente capacite,
                                         Instant maintenant,
                                         long nbAutresDemandesProchesDestination,
                                         Double risqueAxeScore) {
        Map<String, Double> criteres = new LinkedHashMap<>();
        criteres.put("KM_APPROCHE", kmApproche(demande.getOrigine(), capacite.getPosition()));
        criteres.put("KM_DETOUR", 0.0);
        criteres.put("ECART_TEMPOREL", ecartTemporelHeures(
                demande.getFenetreDebut(), demande.getFenetreFin(), maintenant));
        criteres.put("GAIN_REMPLISSAGE",
                gainRemplissage(demande.getPoidsTaxableKg(), capacite.getCapaciteResiduelleKg()));
        criteres.put("FIABILITE", fiabilite(capacite.getValeursCriteres()));
        criteres.put("RISQUE_AXE", risqueAxeScore == null ? 0.0 : risqueAxeScore);
        criteres.put("VALEUR_RETOUR",
                valeurRetour(nbAutresDemandesProchesDestination, valeurRetourSaturation));
        return criteres;
    }

    /**
     * Distance a vide camion -> point d'enlevement (Haversine, km). Donnee
     * manquante (position inconnue) = 0.0 neutre, jamais une penalite inventee
     * (convention ENF-DIS-04 du fichier : on degrade, on n'exclut pas).
     */
    static double kmApproche(PointGeoDto origineDemande, PointGeoDto positionCapacite) {
        if (origineDemande == null || positionCapacite == null) {
            return 0.0;
        }
        return HaversineUtils.distance(origineDemande, positionCapacite);
    }

    /**
     * Heures d'ecart entre l'instant courant et la fenetre souhaitee de la
     * demande : 0 si maintenant est DANS la fenetre ou si la fenetre est
     * inconnue ; heures d'avance si trop tot ; heures de retard si trop tard.
     */
    static double ecartTemporelHeures(Instant fenetreDebut, Instant fenetreFin, Instant maintenant) {
        if (fenetreDebut == null || fenetreFin == null || maintenant == null) {
            return 0.0;
        }
        if (maintenant.isBefore(fenetreDebut)) {
            return minutesVersHeures(ChronoUnit.MINUTES.between(maintenant, fenetreDebut));
        }
        if (maintenant.isAfter(fenetreFin)) {
            return minutesVersHeures(ChronoUnit.MINUTES.between(fenetreFin, maintenant));
        }
        return 0.0;
    }

    private static double minutesVersHeures(long minutes) {
        return minutes / 60.0;
    }

    /**
     * Part de la capacite residuelle que cette demande occuperait, bornee a
     * 1.0 (une demande plus lourde que le restant ne gagne pas plus qu'un
     * remplissage complet). Donnee manquante ou capacite epuisee = 0.0.
     */
    static double gainRemplissage(BigDecimal poidsTaxableKg, BigDecimal capaciteResiduelleKg) {
        if (poidsTaxableKg == null || capaciteResiduelleKg == null
                || capaciteResiduelleKg.signum() <= 0 || poidsTaxableKg.signum() < 0) {
            return 0.0;
        }
        double ratio = poidsTaxableKg.doubleValue() / capaciteResiduelleKg.doubleValue();
        return Math.min(1.0, Math.max(0.0, ratio));
    }

    /**
     * Relai de l'historique d'execution publie par service-cap. Absent =
     * placeholder neutre (jamais 0, qui penaliserait injustement un transporteur
     * sans historique - le CDC interdit explicitement que la fiabilite devienne
     * "une barriere insurmontable pour un nouvel entrant").
     */
    static double fiabilite(Map<String, Double> valeursCriteresCapacite) {
        if (valeursCriteresCapacite == null) {
            return FIABILITE_DEFAUT_NEUTRE;
        }
        Double fiabilitePubliee = valeursCriteresCapacite.get("FIABILITE");
        return fiabilitePubliee == null ? FIABILITE_DEFAUT_NEUTRE : fiabilitePubliee;
    }

    /**
     * Score [0,1] de probabilite de trouver du fret au retour : saturation
     * atteinte quand nbDemandes >= seuil de saturation, lineaire en dessous.
     */
    static double valeurRetour(long nbDemandesProches, double saturation) {
        if (saturation <= 0) {
            return nbDemandesProches > 0 ? 1.0 : 0.0;
        }
        return Math.min(1.0, nbDemandesProches / saturation);
    }

    public double getValeurRetourRayonKm() {
        return valeurRetourRayonKm;
    }
}
