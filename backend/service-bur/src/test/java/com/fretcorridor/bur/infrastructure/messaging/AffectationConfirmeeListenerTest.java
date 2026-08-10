package com.fretcorridor.bur.infrastructure.messaging;

import com.fretcorridor.bur.domain.MissionAppariee;
import com.fretcorridor.bur.domain.MissionAppparieeService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AffectationConfirmeeListenerTest {

    private static final String TENANT_PHASE1 = "tenant-bgft-douala";

    private final MissionAppparieeService service = mock(MissionAppparieeService.class);
    private final AffectationConfirmeeListener listener = new AffectationConfirmeeListener(service, TENANT_PHASE1);

    @Test
    void maps_the_event_to_a_mission_appariee_stamped_with_the_phase1_tenant() {
        UUID eventId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        UUID axeId = UUID.randomUUID();
        UUID transporteurId = UUID.randomUUID();
        AffectationConfirmeeEvent event = evenementExemple(eventId, missionId, axeId, transporteurId);

        listener.ingerer(event);

        verify(service).ingerer(new MissionAppariee(
                missionId, TENANT_PHASE1, axeId, transporteurId,
                "Douala", "Yaoundé", new BigDecimal("50000"), "XAF", event.horodatageConfirmation()), eventId);
    }

    @Test
    void silently_ignores_a_duplicate_event_instead_of_throwing() {
        doThrow(new DataIntegrityViolationException("doublon")).when(service).ingerer(any(), any());

        AffectationConfirmeeEvent event = evenementExemple(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        assertThatCode(() -> listener.ingerer(event)).doesNotThrowAnyException();
    }

    private AffectationConfirmeeEvent evenementExemple(UUID eventId, UUID missionId, UUID axeId, UUID transporteurId) {
        return new AffectationConfirmeeEvent(
                eventId, missionId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), transporteurId,
                UUID.randomUUID(), axeId,
                4.05, 9.71, "Douala",
                3.848, 11.502, "Yaoundé",
                290000.0, 14400, 3600,
                "geometrie-encodee",
                new BigDecimal("50000"), new BigDecimal("5000"), new BigDecimal("45000"), "XAF",
                "DOMICILE", "DOMICILE", Instant.now());
    }
}
