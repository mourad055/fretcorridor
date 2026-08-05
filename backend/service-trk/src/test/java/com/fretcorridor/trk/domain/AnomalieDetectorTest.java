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

class AnomalieDetectorTest {

    private AnomalieDetector detector;
    private UUID missionId;

    @BeforeEach
    void setUp() {
        detector = new AnomalieDetector();
        missionId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Aucune anomalie si liste vide")
    void listeVide() {
        var r = detector.detecter(missionId, List.of());
        assertThat(r.anomalieDetectee()).isFalse();
    }

    @Test
    @DisplayName("Aucune anomalie si liste null")
    void listeNull() {
        var r = detector.detecter(missionId, null);
        assertThat(r.anomalieDetectee()).isFalse();
    }

    @Test
    @DisplayName("Detecte absence prolongee de position")
    void absenceProlongee() {
        Instant vieille = Instant.now().minus(3, ChronoUnit.HOURS);
        Position p = pos(vieille, 4.05, 9.76);
        var r = detector.detecter(missionId, List.of(p));

        assertThat(r.anomalieDetectee()).isTrue();
        assertThat(r.absenceProlongee()).isTrue();
    }

    @Test
    @DisplayName("Pas d'absence si position recente")
    void positionRecente() {
        Position p = pos(Instant.now(), 4.05, 9.76);
        var r = detector.detecter(missionId, List.of(p));
        assertThat(r.absenceProlongee()).isFalse();
    }

    @Test
    @DisplayName("Detecte saut aberrant entre 2 positions")
    void sautAberrant() {
        Instant t1 = Instant.now().minus(1, ChronoUnit.MINUTES);
        Instant t2 = Instant.now();
        Position p1 = pos(t1, 4.05, 9.76);
        Position p2 = pos(t2, 3.85, 11.50); // ~200 km en 1 minute = impossible
        var r = detector.detecter(missionId, List.of(p1, p2));

        assertThat(r.anomalieDetectee()).isTrue();
        assertThat(r.positionAberrante()).isTrue();
    }

    @Test
    @DisplayName("Pas d'anomalie pour positions normales")
    void positionsNormales() {
        Instant t1 = Instant.now().minus(5, ChronoUnit.MINUTES);
        Instant t2 = Instant.now();
        Position p1 = pos(t1, 4.05, 9.76);
        Position p2 = pos(t2, 4.04, 9.78);
        var r = detector.detecter(missionId, List.of(p1, p2));

        assertThat(r.anomalieDetectee()).isFalse();
        assertThat(r.description()).isEqualTo("Aucune anomalie");
    }

    @Test
    @DisplayName("La description liste les types d'anomalies")
    void descriptionComplete() {
        Instant vieille = Instant.now().minus(4, ChronoUnit.HOURS);
        Position p = pos(vieille, 4.05, 9.76);
        var r = detector.detecter(missionId, List.of(p));

        assertThat(r.description()).contains("absence");
    }

    private Position pos(Instant t, double lat, double lon) {
        return new Position(UUID.randomUUID(), missionId, UUID.randomUUID(),
                lat, lon, "GPS", 10.0, t, t);
    }
}
