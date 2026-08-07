package com.fretcorridor.opt.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduit entre le DTO interne du Moteur (ItineraireRequestDto/ResponseDto,
 * stable) et le format JSON exact de l'API Valhalla (ValhallaRouteRequest/
 * Response, calque sur la doc officielle). Isole ValhallaClient de tout
 * detail du contrat externe - si Valhalla change de format demain, seul ce
 * mapper change.
 */
@Component
class ValhallaRequestMapper {

    private static final Logger log = LoggerFactory.getLogger(ValhallaRequestMapper.class);

    private static final String UNITS = "kilometers";
    private static final String COSTING_TRUCK = "truck";

    /**
     * Construit la requete Valhalla. costing_options.truck ne contient que
     * les champs renseignes du profil (CDC S8.11.2 : completude faible et
     * non documentee sur les donnees africaines - profil partiel = defauts
     * Valhalla pour le reste, jamais un echec).
     */
    ValhallaRouteRequest versValhalla(ItineraireRequestDto requete) {
        List<ValhallaRouteRequest.ValhallaLocation> locations = requete.points().stream()
                .map(point -> new ValhallaRouteRequest.ValhallaLocation(point.latitude(), point.longitude()))
                .toList();

        Map<String, Object> optionsTruck = construireOptionsTruck(requete.profilCamion());
        Map<String, Map<String, Object>> costingOptions = optionsTruck.isEmpty()
                ? null
                : Map.of(COSTING_TRUCK, optionsTruck);

        return new ValhallaRouteRequest(locations, COSTING_TRUCK, costingOptions, UNITS);
    }

    private Map<String, Object> construireOptionsTruck(ProfilCamionDto profil) {
        Map<String, Object> options = new HashMap<>();
        if (profil == null) {
            return options;
        }
        if (profil.hauteurMetres() != null) options.put("height", profil.hauteurMetres());
        if (profil.largeurMetres() != null) options.put("width", profil.largeurMetres());
        if (profil.longueurMetres() != null) options.put("length", profil.longueurMetres());
        if (profil.poidsMaxTonnes() != null) options.put("weight", profil.poidsMaxTonnes());
        if (profil.chargeMaxParEssieuTonnes() != null) options.put("axle_load", profil.chargeMaxParEssieuTonnes());
        if (profil.nombreEssieux() != null) options.put("axle_count", profil.nombreEssieux());
        // hazmat est primitif (non nullable) : toujours transmis, contrairement aux autres.
        options.put("hazmat", profil.matieresDangereuses());
        return options;
    }

    /**
     * Traduit la reponse Valhalla vers le DTO interne. margeRatio vient de
     * ValhallaClientProperties (decision d'equipe, pas une valeur du CDC -
     * cf. commentaire sur ce champ dans ValhallaClientProperties).
     *
     * LIMITATION MVP ASSUMEE : la geometrie encodee (geometrieEncodee)
     * n'est fiable que pour un trajet a un seul leg (2 points, cas du V0 -
     * pas de multi-arret avant le sequencement L2 en Phase 2). Concatener
     * naivement les shapes de plusieurs legs produirait une polyline
     * invalide (chaque encodage Valhalla est delta-compresse depuis son
     * propre point de depart) - on retourne null plutot qu'une geometrie
     * fausse, avec un avertissement explicite.
     */
    ItineraireResponseDto mapVersDto(ValhallaRouteResponse reponse, double margeRatio) {
        ValhallaRouteResponse.ValhallaTrip trip = reponse.trip();
        if (trip == null || trip.legs() == null || trip.legs().isEmpty()) {
            log.warn("Reponse Valhalla sans trip/legs exploitable - traite comme un echec");
            return null;
        }

        double distanceMetres = trip.summary().length() * 1000.0;
        double dureeSecondes = trip.summary().time();
        double intervalleConfianceSecondes = dureeSecondes * margeRatio;

        String geometrieEncodee;
        if (trip.legs().size() == 1) {
            geometrieEncodee = trip.legs().get(0).shape();
        } else {
            log.warn("Trajet a {} legs - geometrie multi-leg non agregee en MVP (Phase 2 avec le "
                    + "sequencement L2), geometrieEncodee renvoyee a null pour ce calcul", trip.legs().size());
            geometrieEncodee = null;
        }

        return new ItineraireResponseDto(distanceMetres, dureeSecondes, intervalleConfianceSecondes, geometrieEncodee);
    }
}
