package com.fretcorridor.pay.infrastructure.messaging;

import com.fretcorridor.pay.domain.SequestreInvalideException;
import com.fretcorridor.pay.domain.SequestreService;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** RG-078, Item A de docs/DEPENDANCES_MOBILE_PHASE4.md : libère le séquestre à la réception de MissionLivree (service-exe). */
class MissionLivreeListenerTest {

    private final SequestreService sequestreService = mock(SequestreService.class);
    private final MissionLivreeListener listener = new MissionLivreeListener(sequestreService);

    @Test
    void delegates_to_sequestre_service_liberer_with_the_event_fields() {
        MissionLivreeEvent event = new MissionLivreeEvent(
                "event-1", "mission-1", "tenant-1", "actor-transporteur-1", "etape-livraison-42",
                Instant.parse("2026-08-18T09:00:00Z"));

        listener.ingerer(event);

        verify(sequestreService).liberer("mission-1", "tenant-1", "actor-transporteur-1", "etape-livraison-42");
    }

    /** Kafka livre au moins une fois : un rejeu du même événement ne doit jamais faire planter le consommateur. */
    @Test
    void swallows_sequestre_invalide_exception_on_replay_instead_of_propagating() {
        MissionLivreeEvent event = new MissionLivreeEvent(
                "event-1", "mission-1", "tenant-1", "actor-transporteur-1", "etape-livraison-42",
                Instant.parse("2026-08-18T09:00:00Z"));
        doThrow(new SequestreInvalideException("Le séquestre de la mission mission-1 n'est pas dans l'état DECLENCHE"))
                .when(sequestreService).liberer("mission-1", "tenant-1", "actor-transporteur-1", "etape-livraison-42");

        listener.ingerer(event);
    }
}
