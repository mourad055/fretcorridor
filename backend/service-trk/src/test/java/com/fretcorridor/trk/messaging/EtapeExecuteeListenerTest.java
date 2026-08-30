package com.fretcorridor.trk.messaging;

import com.fretcorridor.trk.domain.ColisRecuperation;
import com.fretcorridor.trk.domain.ColisRecuperationRepository;
import com.fretcorridor.trk.domain.Position;
import com.fretcorridor.trk.domain.PositionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de EtapeExecuteeListener (point 6 du plan de reorientation : "colis
 * recupere = position chauffeur"). Verifie que seul l'enlevement cree
 * l'etat de recuperation, et que la PK mission_id assure l'idempotence face
 * aux doublons Kafka.
 */
class EtapeExecuteeListenerTest {

    private final ColisRecuperationRepository colisRecuperationRepository =
            mock(ColisRecuperationRepository.class);
    private final EtapeExecuteeListener listener =
            new EtapeExecuteeListener(colisRecuperationRepository);

    @Test
    void enlevement_cree_letat_de_recuperation() {
        UUID missionId = UUID.randomUUID();
        Instant horodatage = Instant.now();

        listener.enregistrerEnlevement(new EtapeExecuteeEvent(
                UUID.randomUUID(), missionId, EtapeExecuteeEvent.TypeEtape.ENLEVEMENT, horodatage));

        verify(colisRecuperationRepository).save(any(ColisRecuperation.class));
    }

    @Test
    void livraison_ne_cree_pas_letat_de_recuperation() {
        listener.enregistrerEnlevement(new EtapeExecuteeEvent(
                UUID.randomUUID(), UUID.randomUUID(), EtapeExecuteeEvent.TypeEtape.LIVRAISON, null));

        verify(colisRecuperationRepository, never()).save(any(ColisRecuperation.class));
    }

    @Test
    void doublon_kafka_ignore_sans_erreur() {
        when(colisRecuperationRepository.save(any(ColisRecuperation.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        // Ne doit pas remonter : un retry reseau cote EXE ne doit jamais faire
        // echouer le traitement (idempotence par PK mission_id).
        listener.enregistrerEnlevement(new EtapeExecuteeEvent(
                UUID.randomUUID(), UUID.randomUUID(), EtapeExecuteeEvent.TypeEtape.ENLEVEMENT, Instant.now()));
    }

    @Test
    void horodatage_execution_absent_fallbacksur_le_present() {
        when(colisRecuperationRepository.save(any(ColisRecuperation.class))).thenReturn(null);

        listener.enregistrerEnlevement(new EtapeExecuteeEvent(
                UUID.randomUUID(), UUID.randomUUID(), EtapeExecuteeEvent.TypeEtape.ENLEVEMENT, null));

        verify(colisRecuperationRepository).save(any(ColisRecuperation.class));
    }
}
