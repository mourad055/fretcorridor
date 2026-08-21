package com.flysoft.fretcorridor.exe.messaging;

import com.flysoft.fretcorridor.exe.entity.Mission;
import com.flysoft.fretcorridor.exe.repository.MissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * S7 : Mission.id EST le missionId de l'événement (clé primaire) — un rejeu
 * du même événement est un no-op silencieux, sans dépendre d'un eventId
 * séparé (contrairement à service-bur, dont la vue est un simple miroir).
 */
class AffectationConfirmeeListenerTest {

    @Mock private MissionRepository missionRepository;
    private AffectationConfirmeeListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new AffectationConfirmeeListener(missionRepository, "tenant-bgft-douala");
    }

    private AffectationConfirmeeEvent evenement(UUID missionId) {
        return evenement(missionId, null);
    }

    private AffectationConfirmeeEvent evenement(UUID missionId, UUID vehiculeId) {
        return new AffectationConfirmeeEvent(
                UUID.randomUUID(), missionId, UUID.randomUUID(), UUID.randomUUID(),
                vehiculeId, UUID.randomUUID(), null, UUID.randomUUID(),
                4.05, 9.7, "Douala", 3.87, 11.52, "Yaoundé",
                300000.0, 18000L, 3600L, null,
                BigDecimal.valueOf(50000), BigDecimal.valueOf(5000), BigDecimal.valueOf(45000),
                "XAF", "PORTE_A_PORTE", "PORTE_A_PORTE", Instant.now(),
                "Sac de ciment", 10, BigDecimal.valueOf(500));
    }

    @Test
    void creates_a_mission_from_the_event_with_the_same_id() {
        UUID missionId = UUID.randomUUID();
        when(missionRepository.existsById(missionId)).thenReturn(false);

        listener.ingerer(evenement(missionId));

        ArgumentCaptor<Mission> captor = ArgumentCaptor.forClass(Mission.class);
        verify(missionRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(missionId);
        assertThat(captor.getValue().getTenantId()).isEqualTo("tenant-bgft-douala");
        assertThat(captor.getValue().getStatut()).isEqualTo(Mission.StatutMission.EN_ATTENTE);
    }

    @Test
    void creates_a_mission_with_the_assigned_vehicle_id() {
        UUID missionId = UUID.randomUUID();
        UUID vehiculeId = UUID.randomUUID();
        when(missionRepository.existsById(missionId)).thenReturn(false);

        listener.ingerer(evenement(missionId, vehiculeId));

        ArgumentCaptor<Mission> captor = ArgumentCaptor.forClass(Mission.class);
        verify(missionRepository).save(captor.capture());
        assertThat(captor.getValue().getVehiculeId()).isEqualTo(vehiculeId);
    }

    @Test
    void replaying_the_same_event_is_a_silent_no_op() {
        UUID missionId = UUID.randomUUID();
        when(missionRepository.existsById(missionId)).thenReturn(true);

        listener.ingerer(evenement(missionId));

        verify(missionRepository, never()).save(any());
    }
}
