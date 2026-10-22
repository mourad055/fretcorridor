package com.fretcorridor.geo.web;

import com.fretcorridor.geo.domain.Axe;
import com.fretcorridor.geo.domain.AxeRepository;
import com.fretcorridor.geo.domain.AxeResolutionService;
import com.fretcorridor.geo.domain.Hub;
import com.fretcorridor.geo.domain.HubRepository;
import com.fretcorridor.geo.domain.JournalAuditConventionRepository;
import com.fretcorridor.geo.domain.JournalAuditRisqueRepository;
import com.fretcorridor.geo.domain.TypeHub;
import com.fretcorridor.geo.web.dto.AxeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AxeControllerResolutionTest {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Mock
    private AxeRepository axeRepository;

    @Mock
    private HubRepository hubRepository;

    @Mock
    private JournalAuditRisqueRepository journalAuditRisqueRepository;

    @Mock
    private JournalAuditConventionRepository journalAuditConventionRepository;

    @Mock
    private AxeResolutionService axeResolutionService;

    @InjectMocks
    private AxeController axeController;

    @Test
    void renvoie200AvecLAxeResolu() {
        Axe axe = axeDoualaYaounde();
        when(axeResolutionService.resoudre(4.05, 9.76, 3.84, 11.50, null))
                .thenReturn(Optional.of(axe));

        ResponseEntity<AxeResponse> reponse =
                axeController.resoudreAxe(4.05, 9.76, 3.84, 11.50, null);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).isNotNull();
        assertThat(reponse.getBody().nom()).isEqualTo("Douala-Yaounde");
    }

    @Test
    void renvoie204QuandAucunAxeNEstTrouve() {
        when(axeResolutionService.resoudre(4.05, 9.76, 3.84, 11.50, null))
                .thenReturn(Optional.empty());

        ResponseEntity<AxeResponse> reponse =
                axeController.resoudreAxe(4.05, 9.76, 3.84, 11.50, null);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(reponse.getBody()).isNull();
    }

    @Test
    void rejetteDesCoordonneesHorsBornes() {
        assertThatThrownBy(() -> axeController.resoudreAxe(91.0, 9.76, 3.84, 11.50, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(axeResolutionService);
    }

    private Axe axeDoualaYaounde() {
        Hub douala = new Hub("Hub Douala", "Douala", TypeHub.VILLE,
                GEOMETRY_FACTORY.createPoint(new Coordinate(9.7679, 4.0511)), "cellDouala", "CMR");
        Hub yaounde = new Hub("Hub Yaounde", "Yaounde", TypeHub.VILLE,
                GEOMETRY_FACTORY.createPoint(new Coordinate(11.5021, 3.8480)), "cellYaounde", "CMR");
        return new Axe("Douala-Yaounde", douala, yaounde, "tenant-bgft-douala");
    }
}
