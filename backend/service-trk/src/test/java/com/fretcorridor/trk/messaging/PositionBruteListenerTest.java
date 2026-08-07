package com.fretcorridor.trk.messaging;

import com.fretcorridor.trk.client.AffectationDto;
import com.fretcorridor.trk.client.ServiceOptClient;
import com.fretcorridor.trk.domain.AnomalieDetector;
import com.fretcorridor.trk.domain.EtaCalculator;
import com.fretcorridor.trk.domain.Position;
import com.fretcorridor.trk.domain.PositionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class PositionBruteListenerTest {

    private final PositionRepository positionRepository = mock(PositionRepository.class);
    private final EtaCalculator etaCalculator = mock(EtaCalculator.class);
    private final AnomalieDetector anomalieDetector = mock(AnomalieDetector.class);
    private final TrkEventPublisher eventPublisher = mock(TrkEventPublisher.class);
    private final ServiceOptClient serviceOptClient = mock(ServiceOptClient.class);
    private final PositionBruteListener listener = new PositionBruteListener(
            positionRepository, etaCalculator, anomalieDetector, eventPublisher, serviceOptClient);

    @Test
    void ingerePositionValideSansErreur() {
        PositionBruteEvent event = evenementExemple();

        // Mocker save()
        when(positionRepository.save(any(Position.class))).thenReturn(null);

        // Mocker ServiceOptClient : sans ce stub, obtenirAffectation() renvoie
        // Optional.empty() par defaut (mock non stubbe) et le calcul d'ETA
        // serait court-circuite - on teste ici le chemin nominal complet,
        // celui du bug corrige (destination reelle via service-opt, plus
        // jamais la position courante du vehicule en substitut).
        AffectationDto affectation = new AffectationDto(
                UUID.randomUUID(), 4.05, 9.71, 3.848, 11.502); // Douala -> Yaounde
        when(serviceOptClient.obtenirAffectation(any())).thenReturn(Optional.of(affectation));

        // Mocker EtaCalculator pour retourner un resultat valide
        Instant now = Instant.now();
        when(etaCalculator.calculer(any(), anyDouble(), anyDouble()))
                .thenReturn(new EtaCalculator.EtaResultat(
                        now.plus(3, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS), now.plus(5, ChronoUnit.HOURS),
                        200.0, 60.0, 5, now));

        // Mocker AnomalieDetector
        when(anomalieDetector.detecter(any(), any()))
                .thenReturn(new AnomalieDetector.ResultatDetection(
                        false, false, false, false, false,
                        Duration.ZERO, null));

        assertThatCode(() -> listener.ingerer(event)).doesNotThrowAnyException();
        verify(positionRepository, times(1)).save(any(Position.class));
    }

    @Test
    void ignoreSilencieusementUnDoublon() {
        PositionBruteEvent event = evenementExemple();
        when(positionRepository.save(any(Position.class)))
                .thenThrow(new DataIntegrityViolationException("doublon simule"));

        assertThatCode(() -> listener.ingerer(event)).doesNotThrowAnyException();
        verify(positionRepository, times(1)).save(any(Position.class));
    }

    private PositionBruteEvent evenementExemple() {
        return new PositionBruteEvent(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 4.05, 9.71, "GPS_NATIF", 12.5,
                Instant.now(), Instant.now());
    }
}
