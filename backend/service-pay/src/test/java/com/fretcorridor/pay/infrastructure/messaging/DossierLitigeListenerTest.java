package com.fretcorridor.pay.infrastructure.messaging;

import com.fretcorridor.pay.domain.LitigeMission;
import com.fretcorridor.pay.domain.LitigeMissionPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DossierLitigeListenerTest {

    private final LitigeMissionPort litigeMissionPort = mock(LitigeMissionPort.class);
    private final DossierLitigeListener listener = new DossierLitigeListener(litigeMissionPort);

    @Test
    void maps_the_event_to_a_litige_mission_and_delegates_to_the_upsert_if_newer_port() {
        Instant horodatage = Instant.parse("2026-08-12T09:00:00Z");
        DossierLitigeEvent event = new DossierLitigeEvent(
                "event-1", "dossier-1", "tenant-1", "mission-1", true, horodatage);

        listener.ingerer(event);

        verify(litigeMissionPort).enregistrerSiPlusRecent(new LitigeMission("mission-1", "tenant-1", true, horodatage));
    }
}
