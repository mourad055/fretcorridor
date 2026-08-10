package com.fretcorridor.bur.infrastructure.messaging;

import com.fretcorridor.bur.domain.PositionService;
import com.fretcorridor.bur.domain.PositionVehicule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme PositionETA (service-trk, Moteur). Naturellement idempotent :
 * PositionRepositoryAdapter.enregistrerSiPlusRecente ignore tout événement
 * dont l'horodatage n'est pas strictement postérieur à la position déjà
 * connue pour la mission — un rejeu exact ou un message en retard ne
 * provoque jamais d'écriture (pas besoin d'une contrainte d'unicité sur
 * eventId comme AffectationConfirmeeListener).
 *
 * {@code tenantIdPhase1} : même raisonnement mono-tenant Phase 1 que
 * AffectationConfirmeeListener (cf. ADR 0011/0013) — l'événement ne porte
 * aucun tenant.
 */
@Component
public class PositionEtaListener {

    private static final Logger log = LoggerFactory.getLogger(PositionEtaListener.class);

    private final PositionService service;
    private final String tenantIdPhase1;

    public PositionEtaListener(PositionService service,
                                @Value("${fretcorridor.bur.tenant-id-phase1}") String tenantIdPhase1) {
        this.service = service;
        this.tenantIdPhase1 = tenantIdPhase1;
    }

    @KafkaListener(topics = "position-eta", containerFactory = "positionEtaKafkaListenerContainerFactory")
    public void ingerer(PositionEtaEvent event) {
        service.ingerer(new PositionVehicule(
                event.missionId(), tenantIdPhase1, event.vehiculeId(),
                event.derniereLatitude(), event.derniereLongitude(), event.horodatageDernierePosition()));
        log.debug("Position mise à jour - mission={}, eventId={}", event.missionId(), event.eventId());
    }
}
