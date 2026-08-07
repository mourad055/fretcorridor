package com.fretcorridor.opt.client;

import java.util.List;

/**
 * Contrat JSON EXACT renvoye par Valhalla POST /route. trip.summary agrege
 * deja length/time sur l'ensemble du trajet (tous legs confondus) - on
 * l'utilise plutot que de resommer les legs a la main, moins de risque
 * d'erreur. La geometrie (shape) reste par leg : cf. limitation MVP
 * documentee dans ValhallaRequestMapper.mapVersDto.
 */
record ValhallaRouteResponse(ValhallaTrip trip) {

    record ValhallaTrip(ValhallaSummary summary, List<ValhallaLeg> legs) {
    }

    record ValhallaSummary(double length, double time) {
    }

    record ValhallaLeg(ValhallaSummary summary, String shape) {
    }
}
