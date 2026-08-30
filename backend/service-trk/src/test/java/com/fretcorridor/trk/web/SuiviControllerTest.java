package com.fretcorridor.trk.web;

import com.fretcorridor.trk.client.AffectationDto;
import com.fretcorridor.trk.client.ServiceOptClient;
import com.fretcorridor.trk.domain.ColisRecuperation;
import com.fretcorridor.trk.domain.ColisRecuperationRepository;
import com.fretcorridor.trk.domain.Position;
import com.fretcorridor.trk.domain.PositionRepository;
import com.fretcorridor.trk.web.dto.SuiviMissionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests de SuiviController (point 6 du plan de reorientation) : la position a
 * afficher bascule de POSITION_ESTIMEE (point d'enlevement) vers
 * GPS_CHAUFFEUR (position temps reel) des que le colis est recupere.
 */
class SuiviControllerTest {

    private final ColisRecuperationRepository colisRecuperationRepository =
            mock(ColisRecuperationRepository.class);
    private final PositionRepository positionRepository = mock(PositionRepository.class);
    private final ServiceOptClient serviceOptClient = mock(ServiceOptClient.class);
    private final SuiviController controller = new SuiviController(
            colisRecuperationRepository, positionRepository, serviceOptClient);

    private final UUID missionId = UUID.randomUUID();

    @Test
    void colis_non_recupere_affiche_la_position_estimee_enlevement() {
        when(colisRecuperationRepository.findFirstByMissionId(missionId)).thenReturn(Optional.empty());
        when(serviceOptClient.obtenirAffectation(missionId))
                .thenReturn(Optional.of(new AffectationDto(missionId, 4.05, 9.70, 3.85, 11.50)));

        SuiviMissionResponse reponse = controller.consulter(missionId);

        assertFalse(reponse.colisRecupere());
        assertEquals(SuiviMissionResponse.SourcePosition.POSITION_ESTIMEE, reponse.sourcePosition());
        assertEquals(4.05, reponse.latitude());
        assertEquals(9.70, reponse.longitude());
    }

    @Test
    void colis_recupere_affiche_la_position_gps_du_chauffeur() {
        Instant enlevement = Instant.parse("2026-08-29T10:00:00Z");
        Instant capture = Instant.parse("2026-08-29T11:30:00Z");
        when(colisRecuperationRepository.findFirstByMissionId(missionId))
                .thenReturn(Optional.of(new ColisRecuperation(missionId, enlevement)));
        Position position = new Position(
                UUID.randomUUID(), missionId, UUID.randomUUID(), 4.25, 10.10, "GPS", 5.0, capture, capture);
        when(positionRepository.findFirstByMissionIdOrderByHorodatageCaptureDesc(missionId))
                .thenReturn(Optional.of(position));

        SuiviMissionResponse reponse = controller.consulter(missionId);

        assertTrue(reponse.colisRecupere());
        assertEquals(SuiviMissionResponse.SourcePosition.GPS_CHAUFFEUR, reponse.sourcePosition());
        assertEquals(4.25, reponse.latitude());
        assertEquals(10.10, reponse.longitude());
        assertEquals(enlevement, reponse.horodatageEnlevement());
    }

    @Test
    void colis_recupere_sans_gps_leve_503() {
        when(colisRecuperationRepository.findFirstByMissionId(missionId))
                .thenReturn(Optional.of(new ColisRecuperation(missionId, Instant.now())));
        when(positionRepository.findFirstByMissionIdOrderByHorodatageCaptureDesc(missionId))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> controller.consulter(missionId));
    }

    @Test
    void mission_inconnue_sans_colis_leve_404() {
        when(colisRecuperationRepository.findFirstByMissionId(missionId)).thenReturn(Optional.empty());
        when(serviceOptClient.obtenirAffectation(missionId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> controller.consulter(missionId));
    }
}
