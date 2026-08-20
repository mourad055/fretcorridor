package com.flysoft.fretcorridor.flt.service;

import com.flysoft.fretcorridor.flt.client.ServiceExeClient;
import com.flysoft.fretcorridor.flt.dto.PositionDto;
import com.flysoft.fretcorridor.flt.entity.Position;
import com.flysoft.fretcorridor.flt.messaging.FltEventPublisher;
import com.flysoft.fretcorridor.flt.messaging.PositionBruteEvent;
import com.flysoft.fretcorridor.flt.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PositionServiceTest {

    @Mock private PositionRepository positionRepository;
    @Mock private ServiceExeClient serviceExeClient;
    @Mock private FltEventPublisher eventPublisher;

    private PositionService service;
    private UUID missionId;
    private static final String TENANT = "tenant-bgft-douala";
    private static final String AUTH_HEADER = "Bearer un-jwt-quelconque";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PositionService(positionRepository, serviceExeClient, eventPublisher);
        missionId = UUID.randomUUID();
    }

    private PositionDto.EnvoyerRequest requete() {
        var requete = new PositionDto.EnvoyerRequest();
        requete.setMissionId(missionId);
        requete.setLatitude(4.05);
        requete.setLongitude(9.7);
        requete.setHorodatage(LocalDateTime.now());
        return requete;
    }

    @Test
    void recording_a_position_with_a_resolved_vehicle_publishes_it_to_the_engine() {
        UUID vehiculeId = UUID.randomUUID();
        when(serviceExeClient.resoudreVehicule(missionId, AUTH_HEADER)).thenReturn(Optional.of(vehiculeId));

        service.enregistrer(requete(), TENANT, AUTH_HEADER);

        verify(positionRepository).save(any(Position.class));

        ArgumentCaptor<PositionBruteEvent> captor = ArgumentCaptor.forClass(PositionBruteEvent.class);
        verify(eventPublisher).publierPositionBrute(captor.capture());
        assertThat(captor.getValue().missionId()).isEqualTo(missionId);
        assertThat(captor.getValue().vehiculeId()).isEqualTo(vehiculeId);
    }

    @Test
    void recording_a_position_without_a_resolved_vehicle_is_saved_but_never_published() {
        when(serviceExeClient.resoudreVehicule(missionId, AUTH_HEADER)).thenReturn(Optional.empty());

        service.enregistrer(requete(), TENANT, AUTH_HEADER);

        verify(positionRepository).save(any(Position.class));
        verify(eventPublisher, never()).publierPositionBrute(any());
    }
}
