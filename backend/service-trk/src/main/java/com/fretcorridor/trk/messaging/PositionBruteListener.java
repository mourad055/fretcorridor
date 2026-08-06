package com.fretcorridor.trk.messaging;

import com.fretcorridor.trk.domain.AnomalieDetector;
import com.fretcorridor.trk.domain.EtaCalculator;
import com.fretcorridor.trk.domain.Position;
import com.fretcorridor.trk.domain.PositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class PositionBruteListener {

    private static final Logger log = LoggerFactory.getLogger(PositionBruteListener.class);

    private final PositionRepository positionRepository;
    private final EtaCalculator etaCalculator;
    private final AnomalieDetector anomalieDetector;
    private final TrkEventPublisher eventPublisher;

    public PositionBruteListener(PositionRepository positionRepository,
                                 EtaCalculator etaCalculator,
                                 AnomalieDetector anomalieDetector,
                                 TrkEventPublisher eventPublisher) {
        this.positionRepository = positionRepository;
        this.etaCalculator = etaCalculator;
        this.anomalieDetector = anomalieDetector;
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(topics = "position-brute", groupId = "service-trk")
    public void ingerer(PositionBruteEvent event) {
        Position position = new Position(
                event.eventId(),
                event.missionId(),
                event.vehiculeId(),
                event.latitude(),
                event.longitude(),
                event.sourceCapture(),
                event.precisionMetres(),
                event.horodatageCapture(),
                event.horodatageTransmission()
        );

        try {
            positionRepository.save(position);
            log.debug("Position ingérée - mission={}, vehicule={}, eventId={}",
                    event.missionId(), event.vehiculeId(), event.eventId());
            traiterPostIngestion(position);
        } catch (DataIntegrityViolationException doublon) {
            log.info("Position déjà ingérée, doublon ignoré - eventId={}", event.eventId());
        }
    }

    private void traiterPostIngestion(Position dernierePosition) {
        UUID missionId = dernierePosition.getMissionId();
        UUID vehiculeId = dernierePosition.getVehiculeId();

        List<Position> positions = positionRepository.findByMissionIdOrderByHorodatageCaptureAsc(missionId);

        // ETA
        EtaCalculator.EtaResultat eta = etaCalculator.calculer(
                positions,
                dernierePosition.getLatitude(),  // Phase 2 : destination réelle
                dernierePosition.getLongitude()
        );

        if (eta != null && eta.isDisponible()) {
            PositionEtaEvent etaEvent = new PositionEtaEvent(
                    UUID.randomUUID(),
                    missionId,
                    vehiculeId,
                    dernierePosition.getLatitude(),
                    dernierePosition.getLongitude(),
                    dernierePosition.getHorodatageCapture(),
                    eta.distanceRestanteKm(),
                    eta.vitesseEstimeeKmh(),
                    eta.etaCentral(),
                    eta.borneBasse(),
                    eta.borneHaute(),
                    dernierePosition.getSourceCapture(),
                    Instant.now()
            );
            eventPublisher.publierPositionEta(etaEvent);
        }

        // Anomalies
        AnomalieDetector.ResultatDetection detection = anomalieDetector.detecter(missionId, positions);
        if (detection.anomalieDetectee()) {
            AlerteEcartEvent alerteEvent = new AlerteEcartEvent(
                    UUID.randomUUID(),
                    missionId,
                    vehiculeId,
                    typeAnomaliePrincipal(detection),
                    detection.description(),
                    dernierePosition.getLatitude(),
                    dernierePosition.getLongitude(),
                    detection.horodatageDernierePosition(),
                    detection.ageDernierePosition().getSeconds(),
                    dernierePosition.getSourceCapture(),
                    Instant.now()
            );
            eventPublisher.publierAlerteEcart(alerteEvent);
        }
    }

    private String typeAnomaliePrincipal(AnomalieDetector.ResultatDetection detection) {
        if (detection.absenceProlongee()) return "ABSENCE_PROLONGEE";
        if (detection.arretProlonge()) return "ARRET_PROLONGE";
        if (detection.positionAberrante()) return "POSITION_ABERRANTE";
        if (detection.ecartCorridor()) return "ECART_CORRIDOR";
        return "INCONNU";
    }
}
