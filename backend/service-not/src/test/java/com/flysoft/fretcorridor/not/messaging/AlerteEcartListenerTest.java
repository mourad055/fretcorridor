package com.flysoft.fretcorridor.not.messaging;

import com.flysoft.fretcorridor.not.client.ServiceFltClient;
import com.flysoft.fretcorridor.not.entity.Notification;
import com.flysoft.fretcorridor.not.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AlerteEcartListenerTest {

    @Mock private ServiceFltClient serviceFltClient;
    @Mock private NotificationService notificationService;

    private AlerteEcartListener listener;
    private static final String TENANT = "tenant-bgft-douala";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new AlerteEcartListener(serviceFltClient, notificationService, TENANT);
    }

    private AlerteEcartEvent evenement(UUID missionId, UUID vehiculeId) {
        return new AlerteEcartEvent(
                UUID.randomUUID(), missionId, vehiculeId, "ECART_CORRIDOR",
                "Déplacement de 250 km hors corridor déclaré",
                4.5, 10.2, Instant.now(), 120L, "MOBILE_CHAUFFEUR", Instant.now());
    }

    @Test
    void a_resolved_vehicle_owner_receives_a_notification() {
        UUID missionId = UUID.randomUUID();
        UUID vehiculeId = UUID.randomUUID();
        UUID transporteurId = UUID.randomUUID();
        when(serviceFltClient.resoudreProprietaire(vehiculeId)).thenReturn(Optional.of(transporteurId));

        listener.ingerer(evenement(missionId, vehiculeId));

        verify(notificationService).creer(
                eq(transporteurId), any(), any(), eq(Notification.TypeNotification.ALERTE_ECART),
                eq(missionId), eq(TENANT));
    }

    @Test
    void an_alert_without_a_vehicle_id_is_never_notified() {
        listener.ingerer(evenement(UUID.randomUUID(), null));

        verifyNoInteractions(serviceFltClient);
        verifyNoInteractions(notificationService);
    }

    @Test
    void an_unresolvable_vehicle_owner_is_never_notified() {
        UUID vehiculeId = UUID.randomUUID();
        when(serviceFltClient.resoudreProprietaire(vehiculeId)).thenReturn(Optional.empty());

        listener.ingerer(evenement(UUID.randomUUID(), vehiculeId));

        verify(notificationService, never()).creer(any(), any(), any(), any(), any(), any());
    }
}
