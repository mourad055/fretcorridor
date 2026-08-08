package com.fretcorridor.opt.client;

import com.fretcorridor.dto.PointGeoDto;

import java.util.Map;
import java.util.UUID;

/**
 * Miroir du contrat PaireCandidatRequest cote service-mat. Pas de code
 * partage entre modules (chaque microservice a son propre build, cf Plan
 * d'execution S4.1) - cette classe est intentionnellement dupliquee, pas
 * importee depuis service-mat.
 *
 * positionCapacite / profilCamion : ajoutes pour le calcul d'itineraire
 * Valhalla en sortie de L1 (README moteur, "Appel au service d'itineraires
 * pour le calcul de trajet", entre L1 et la tarification). Portes directement
 * ici plutot que re-interroges aupres de service-geo : ces donnees sont
 * connues des l'ingestion de l'evenement CapaciteDeclaree (Mobile -> Moteur,
 * cf Plan d'execution S4.2) et le CDC S13 les rattache nativement a l'entite
 * Capacite (origine, vehicule) - inutile de faire un aller-retour GEO pour
 * retrouver une donnee deja disponible.
 *
 * Nullable (comme ProfilCamionDto lui-meme, cf sa javadoc) : un candidat sans
 * position ou profil connu ne doit jamais faire echouer le matching L0/L1 -
 * seul le calcul Valhalla en aval sera degrade pour ce candidat precis.
 */
public record CandidatCoutDto(
        UUID capaciteId,
        Map<String, Double> valeursCriteres,
        PointGeoDto positionCapacite,
        ProfilCamionDto profilCamion,
        // Ajoute pour la Tarification L4 (CDC S8.9.1, COUT_BASE indexe sur
        // "axe, distance, type de vehicule" en regime forfaitaire). Chaine
        // libre, pas d'enum Java : coherent avec code_service/code_critere
        // ailleurs dans le moteur - un nouveau type de vehicule ne doit
        // jamais exiger de redeploiement. Nullable : un candidat sans type
        // connu degrade la tarification (cf TarificationL4Service), ne la
        // fait jamais echouer.
        String typeVehicule
) {
}
