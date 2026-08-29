package com.fretcorridor.opt.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Point 3 (plan de reorientation) : multi-legs Valhalla + points d'arret.
 * Verifie le roundtrip encode/decode de PolylineUtil et l'agregation de la
 * geometrie multi-legs (concatenation des shapes via re-encodage, plus de
 * retour null pour un trajet a N legs).
 */
class PolylineUtilTest {

    @Test
    void roundtrip_assoit_la_polyline_simplementUneSerieDePoints() {
        double[][] entrees = {{4.05, 9.70}, {4.10, 9.75}, {3.87, 11.52}};
        String encodee = PolylineUtil.encoder(entrees);
        double[][] decodee = PolylineUtil.decoder(encodee);

        assertThat(decodee.length).isEqualTo(entrees.length);
        for (int i = 0; i < entrees.length; i++) {
            assertThat(decodee[i][0]).isCloseTo(entrees[i][0], org.assertj.core.data.Offset.offset(1e-5));
            assertThat(decodee[i][1]).isCloseTo(entrees[i][1], org.assertj.core.data.Offset.offset(1e-5));
        }
    }

    @Test
    void decoder_surUneChaineVide_retourneUneListeVide_sansException() {
        assertThat(PolylineUtil.decoder("")).isEmpty();
    }

    @Test
    void agregationMultiLegs_fusionneLesShapesSansPointDeJonctionDuplique() {
        double[][] leg1 = {{4.05, 9.70}, {4.06, 9.71}};
        double[][] leg2 = {{4.06, 9.71}, {4.07, 9.72}};
        String shape1 = PolylineUtil.encoder(leg1);
        String shape2 = PolylineUtil.encoder(leg2);

        ValhallaRouteResponse reponse = new ValhallaRouteResponse(new ValhallaRouteResponse.ValhallaTrip(
                new ValhallaRouteResponse.ValhallaSummary(30.0, 1800.0),
                List.of(
                        new ValhallaRouteResponse.ValhallaLeg(
                                new ValhallaRouteResponse.ValhallaSummary(15.0, 900.0), shape1),
                        new ValhallaRouteResponse.ValhallaLeg(
                                new ValhallaRouteResponse.ValhallaSummary(15.0, 900.0), shape2)
                )));

        ValhallaRequestMapper mapper = new ValhallaRequestMapper();
        ItineraireResponseDto dto = mapper.mapVersDto(reponse, 0.2);

        assertThat(dto).isNotNull();
        // 3 points distincts (4.05,9.70)(4.06,9.71)(4.07,9.72) - le point
        // central de jonction n'est pas duplique.
        assertThat(PolylineUtil.decoder(dto.geometrieEncodee()).length).isEqualTo(3);
        // La distance/duree agregees par Valhalla sont conservees telles quelles.
        assertThat(dto.distanceMetres()).isEqualTo(30000.0);
        assertThat(dto.dureeSecondes()).isEqualTo(1800.0);
        // Intervalle de confiance = duree * margeRatio.
        assertThat(dto.intervalleConfianceSecondes()).isEqualTo(360.0);
    }

    @Test
    void trajetASeulLeg_gardeLaGeometrieEncodeeDirecte() {
        double[][] leg = {{4.05, 9.70}, {3.87, 11.52}};
        String shape = PolylineUtil.encoder(leg);

        ValhallaRouteResponse reponse = new ValhallaRouteResponse(new ValhallaRouteResponse.ValhallaTrip(
                new ValhallaRouteResponse.ValhallaSummary(200.0, 7200.0),
                List.of(new ValhallaRouteResponse.ValhallaLeg(
                        new ValhallaRouteResponse.ValhallaSummary(200.0, 7200.0), shape))));

        ValhallaRequestMapper mapper = new ValhallaRequestMapper();
        ItineraireResponseDto dto = mapper.mapVersDto(reponse, 0.15);

        assertThat(dto).isNotNull();
        assertThat(PolylineUtil.decoder(dto.geometrieEncodee()).length).isEqualTo(2);
    }
}
