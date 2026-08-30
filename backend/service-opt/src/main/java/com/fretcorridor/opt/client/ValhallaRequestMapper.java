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
     * GEOMETRIE MULTI-LEGS (plan de reorientation, partie Chauffeur point 3
     * "multi-legs Valhalla + points d'arret") : une reponse avec plusieurs
     * legs (trajet avec points d'arret intermediaires : position chauffeur ->
     * point d'enlevement -> point de livraison, ou tournee multi-etapes)
     * porte un shape PAR leg, chacun auto-suffisant (encode depuis le point
     * de depart de son leg). On ne concatene plus naivement (polyline
     * invalide, delta-compression par leg) : on decode chaque leg, on
     * fusionne les points en evit le point de jonction duplique, puis on
     * re-encode un seul shape continu. Distance/duree viennent de
     * trip.summary (deja agrege par Valhalla sur tout le trajet).
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

        String geometrieEncodee = agregerGeometrieMultiLegs(trip.legs());

        return new ItineraireResponseDto(distanceMetres, dureeSecondes, intervalleConfianceSecondes, geometrieEncodee);
    }

    /**
     * Agrege les shapes des legs en un seul shape continue. Retourne null
     * (comportement degrade historique) uniquement si aucun leg ne porte de
     * shape exploitable - jamais une geometrie fausse.
     */
    private String agregerGeometrieMultiLegs(List<ValhallaRouteResponse.ValhallaLeg> legs) {
        java.util.List<double[]> tousPoints = new java.util.ArrayList<>();
        double[] precedent = null;
        for (ValhallaRouteResponse.ValhallaLeg leg : legs) {
            if (leg.shape() == null || leg.shape().isEmpty()) {
                continue;
            }
            double[][] pointsLeg = PolylineUtil.decoder(leg.shape());
            for (double[] point : pointsLeg) {
                if (precedent != null
                        && point[0] == precedent[0] && point[1] == precedent[1]) {
                    // Point de jonction duplique entre deux legs consecutifs -
                    // on ne l'ajoute qu'une fois pour garder une polyline valide.
                    continue;
                }
                tousPoints.add(point);
                precedent = point;
            }
        }
        if (tousPoints.isEmpty()) {
            log.warn("Aucun shape de leg exploitable - geometrieEncodee renvoyee a null");
            return null;
        }
        if (tousPoints.size() == 1) {
            return PolylineUtil.encoder(tousPoints.toArray(new double[0][]));
        }
        return PolylineUtil.encoder(tousPoints.toArray(new double[0][]));
    }
}
