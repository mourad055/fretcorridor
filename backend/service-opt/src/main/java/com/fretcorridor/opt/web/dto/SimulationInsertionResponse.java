package com.fretcorridor.opt.web.dto;

/**
 * Reponse de POST /api/opt/simulation-insertion (plan de reorientation,
 * point 4) : l'apercu du nouvel itineraire du chauffeur si une nouvelle
 * demande etait acceptee, sans rien committer.
 *
 * - inseree : true si la demande a trouve une position realisable (capacite +
 *   EF-MAT-10 detour) dans la tournee en cours, false sinon.
 * - detourKm : distance additionnelle engendree par l'insertion (tournee
 *   avec la demande moins tournee sans la demande) - le "nombre de kilometres
 *   ajoute" que le chauffeur doit voir (CDC / plan de reorientation).
 * - tempsAjouteSecondes : duree estimee supplementaire (via Valhalla), peut
 *   etre null en mode degrade (Valhalla indisponible) - le "nombre ajoute
 *   pour arrivee au point d'arret".
 * - tourneeKm / tourneeDureeSecondes : metriques de la tournee complete apres
 *   insertion, pour afficher le nouvel itineraire cumule.
 */
public record SimulationInsertionResponse(
        boolean inseree,
        double detourKm,
        Double tempsAjouteSecondes,
        double tourneeKm,
        Double tourneeDureeSecondes
) {
}
