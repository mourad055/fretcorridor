package com.fretcorridor.trk.messaging;

import com.fretcorridor.trk.domain.Position;
import com.fretcorridor.trk.domain.PositionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifie le comportement d\'idempotence documente dans PositionBruteListener :
 * un doublon (meme event_id, DataIntegrityViolationException levee par la
 * contrainte UNIQUE en base) doit etre ignore silencieusement, jamais
 * propage - sinon Spring Kafka retenterait indefiniment le meme message
 * (poison pill), ce qui bloquerait tout le reste du topic.
 *
 * Test unitaire pur (mock du repository) : pas besoin d\'un vrai Kafka ni
 * d\'une vraie base pour verifier CETTE regle precise - un test d\'integration
 * complet (EmbeddedKafka + Postgres reel) pourra venir plus tard si besoin.
 */
class PositionBruteListenerTest {

    private final PositionRepository positionRepository = mock(PositionRepository.class);
    private final PositionBruteListener listener = new PositionBruteListener(positionRepository);

    @Test
    void ingere_une_position_valide_sans_erreur() {
        PositionBruteEvent event = evenementExemple();

        listener.ingerer(event);

        verify(positionRepository, times(1)).save(any(Position.class));
    }

    @Test
    void ignore_silencieusement_un_doublon_meme_event_id() {
        PositionBruteEvent event = evenementExemple();
        when(positionRepository.save(any(Position.class)))
                .thenThrow(new DataIntegrityViolationException("doublon simule sur event_id"));

        assertThatCode(() -> listener.ingerer(event)).doesNotThrowAnyException();

        verify(positionRepository, times(1)).save(any(Position.class));
    }

    private PositionBruteEvent evenementExemple() {
        return new PositionBruteEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                4.05, 9.71,
                "GPS_NATIF",
                12.5,
                Instant.now(),
                Instant.now()
        );
    }
}
