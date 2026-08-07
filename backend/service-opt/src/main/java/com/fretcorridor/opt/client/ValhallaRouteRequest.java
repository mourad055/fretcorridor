package com.fretcorridor.opt.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Contrat JSON EXACT attendu par l'API Valhalla POST /route (cf. doc
 * officielle Valhalla Turn-by-Turn Route API) - locations/costing/
 * costing_options en snake_case. Ce n'est PAS le DTO interne du Moteur
 * (ItineraireRequestDto) : celui-ci reste stable meme si Valhalla change
 * de format, la traduction se fait dans ValhallaRequestMapper.
 *
 * units="kilometers" fixe pour tout le Moteur - la conversion en metres
 * (distanceMetres) se fait cote ValhallaRequestMapper, jamais devinee
 * ailleurs.
 */
record ValhallaRouteRequest(
        List<ValhallaLocation> locations,
        String costing,
        @JsonProperty("costing_options") Map<String, Map<String, Object>> costingOptions,
        String units
) {
    record ValhallaLocation(double lat, double lon) {
    }
}
