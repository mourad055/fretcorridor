package com.fretcorridor.bur.infrastructure.messaging;

import com.fretcorridor.bur.domain.MissionAppariee;
import com.fretcorridor.bur.domain.MissionAppparieeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme AffectationConfirmee (service-opt, Moteur) — même principe
 * d'idempotence que DemandePublieeListener côté service-opt (unique
 * constraint sur eventId, doublon silencieusement ignoré).
 *
 * {@code tenantIdPhase1} : l'événement ne porte aucun tenant — Phase 1 est
 * mono-tenant (un seul axe/tenant réel, BGFT), même raisonnement que l'ADR
 * 0011 (GEO). À remplacer par une vraie résolution axe→tenant si/quand un
 * second tenant institutionnel rejoint la plateforme (Phase 3).
 */
@Component
public class AffectationConfirmeeListener {

    private static final Logger log = LoggerFactory.getLogger(AffectationConfirmeeListener.class);

    private final MissionAppparieeService service;
    private final String tenantIdPhase1;

    public AffectationConfirmeeListener(MissionAppparieeService service,
                                         @Value("${fretcorridor.bur.tenant-id-phase1}") String tenantIdPhase1) {
        this.service = service;
        this.tenantIdPhase1 = tenantIdPhase1;
    }

    @KafkaListener(topics = "affectation-confirmee", containerFactory = "affectationConfirmeeKafkaListenerContainerFactory")
    public void ingerer(AffectationConfirmeeEvent event) {
        try {
            service.ingerer(new MissionAppariee(
                    event.missionId(), tenantIdPhase1, event.axeId(), event.transporteurId(),
                    event.origineNom(), event.destinationNom(), event.prixTransport(), event.devise(),
                    event.horodatageConfirmation()), event.eventId());
            log.debug("Mission appariée matérialisée - mission={}, eventId={}", event.missionId(), event.eventId());
        } catch (DataIntegrityViolationException doublon) {
            log.info("AffectationConfirmee déjà ingérée, doublon ignoré - eventId={}", event.eventId());
        }
    }
}
