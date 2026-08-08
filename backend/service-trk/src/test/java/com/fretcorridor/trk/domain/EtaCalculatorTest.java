package com.fretcorridor.trk.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class EtaCalculatorTest {

    private EtaCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new EtaCalculator();
    }

    @Test
    @DisplayName("Retourne indisponible si aucune position")
    void sansPositions() {
        var r = calculator.calculer(List.of(), 3.8, 11.5);
        assertThat(r.isDisponible()).isFalse();
    }

    @Test
    @DisplayName("Retourne indisponible si liste null")
    void listeNull() {
        var r = calculator.calculer(null, 3.8, 11.5);
        assertThat(r.isDisponible()).isFalse();
    }

    @Test
    @DisplayName("Calcule ETA avec intervalle de confiance")
    void avecPositionsValides() {
        Instant now = Instant.now();
        Position p1 = pos(now.minus(60, ChronoUnit.SECONDS), 4.05, 9.76);
        Position p2 = pos(now, 4.04, 9.78);
        var r = calculator.calculer(List.of(p1, p2), 3.848, 11.502);

        assertThat(r.isDisponible()).isTrue();
        assertThat(r.etaCentral()).isNotNull();
        assertThat(r.borneBasse()).isNotNull();
        assertThat(r.borneHaute()).isNotNull();
        assertThat(r.distanceRestanteKm()).isGreaterThan(0);
        assertThat(r.vitesseEstimeeKmh()).isGreaterThan(0);

        long ecartBasse = r.etaCentral().getEpochSecond() - r.borneBasse().getEpochSecond();
        long ecartHaute = r.borneHaute().getEpochSecond() - r.etaCentral().getEpochSecond();
        assertThat(ecartHaute).isGreaterThanOrEqualTo(ecartBasse);
    }

    @Test
    @DisplayName("Utilise vitesse minimale avec une seule position")
    void uneSeulePosition() {
        Position seule = pos(Instant.now(), 4.05, 9.76);
        var r = calculator.calculer(List.of(seule), 3.848, 11.502);
        assertThat(r.isDisponible()).isTrue();
        assertThat(r.vitesseEstimeeKmh()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("ETA jamais dans le passe")
    void etaJamaisDansLePasse() {
        Instant now = Instant.now();
        Position p = pos(now, 4.05, 9.76);
        var r = calculator.calculer(List.of(p), 4.05, 9.77);
        assertThat(r.etaCentral()).isAfterOrEqualTo(now);
        assertThat(r.borneBasse()).isAfterOrEqualTo(now);
        assertThat(r.borneHaute()).isAfterOrEqualTo(now);
    }

    private Position pos(Instant t, double lat, double lon) {
        return new Position(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                lat, lon, "GPS", 10.0, t, t);
    }
}
