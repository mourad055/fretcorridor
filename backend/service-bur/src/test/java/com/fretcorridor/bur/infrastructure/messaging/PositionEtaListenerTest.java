package com.fretcorridor.bur.infrastructure.messaging;

import com.fretcorridor.bur.domain.PositionService;
import com.fretcorridor.bur.domain.PositionVehicule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PositionEtaListenerTest {

    private static final String TENANT_PHASE1 = "tenant-bgft-douala";

    private final PositionService service = mock(PositionService.class);
    private final PositionEtaListener listener = new PositionEtaListener(service, TENANT_PHASE1);

    @Test
    void maps_the_event_to_a_position_stamped_with_the_phase1_tenant() {
        UUID missionId = UUID.randomUUID();
        UUID vehiculeId = UUID.randomUUID();
        Instant horodatage = Instant.parse("2026-08-10T12:00:00Z");
        PositionEtaEvent event = new PositionEtaEvent(
                UUID.randomUUID(), missionId, vehiculeId,
                4.05, 9.76, horodatage,
                120.5, 60.0,
                Instant.now(), Instant.now(), Instant.now(),
                "GPS_NATIF", Instant.now());

        listener.ingerer(event);

        verify(service).ingerer(new PositionVehicule(missionId, TENANT_PHASE1, vehiculeId, 4.05, 9.76, horodatage));
    }
}
