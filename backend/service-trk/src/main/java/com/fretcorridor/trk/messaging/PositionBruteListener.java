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

/**
 * Ingestion de PositionBrute (module FLT, Mobile -> TRK, async Kafka).
 *
 * EF-TRK-01/02 : tolérant à la connectivité (auto-offset-reset=earliest).
 *
 * Flux de traitement après ingestion réussie :
 * 1. Persistance de la position (idempotente)
 * 2. Recalcul de l'ETA → publication PositionETA (→ service-exe)
 * 3. Détection d'anomalies → publication AlerteEcart si anomalie (→ service-not)
 *
 * Idempotence (ENF-SEC-03) : contrainte UNIQUE(event_id) en base.
 */
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

            // Après ingestion réussie : ETA + anomalies
            traiterPostIngestion(position);

        } catch (DataIntegrityViolationException doublon) {
            log.info("Position déjà ingérée, doublon ignoré (idempotence) - eventId={}", event.eventId());
        }
    }

    /**
     * Traitements déclenchés après ingestion réussie d'une nouvelle position :
     * - Calcul et publication de l'ETA
     * - Détection et publication d'anomalies
     *
     * Phase 1 (MVP) : utilise uniquement les positions déjà en base.
     * La dépendance à GEO/OPT (itinéraire retenu, axes/hubs) arrivera en Phase 2.
     */
    private void traiterPostIngestion(Position dernierePosition) {
        UUID missionId = dernierePosition.getMissionId();
        UUID vehiculeId = dernierePosition.getVehiculeId();

        // Récupérer l'historique récent des positions de cette mission
        List<Position> positions = positionRepository.findAll()
                .stream()
                .filter(p -> p.getMissionId().equals(missionId))
                .sorted((p1, p2) -> p1.getHorodatageCapture().compareTo(p2.getHorodatageCapture()))
                .toList();

        // --- ETA (EF-TRK-02) ---
        // Note MVP : la destination n'est pas encore fournie par OPT (itinéraire retenu).
        // On utilise la dernière position comme référence ; le vrai calcul viendra en Phase 2
        // quand TRK consommera l'itinéraire depuis OPT.
        // Pour l'instant, on publie l'ETA avec distanceRestanteKm=0 (signal "en attente d'itinéraire").
        EtaCalculator.EtaResultat eta = etaCalculator.calculer(
                positions,
                dernierePosition.getLatitude(),  // TODO Phase 2 : remplacer par destination réelle
                dernierePosition.getLongitude()   // TODO Phase 2 : remplacer par destination réelle
        );

        // Garde-fou defensif (meme principe que ValhallaClient/TarificationL4Service
        // ailleurs dans ce perimetre) : eta ne devrait jamais etre null en
        // production (EtaCalculator renvoie toujours un EtaResultat, y compris
        // "indisponible" via EtaResultat.indisponible(...), jamais une reference
        // null litterale) - mais une future evolution de EtaCalculator (ou un
        // mock de test mal cadre) ne doit jamais faire planter l'ingestion d'une
        // position reelle pour autant. ENF-DIS-04 : le suivi ne doit jamais
        // s'arreter a cause d'un echec du calcul d'ETA.
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

        // --- Détection d'anomalies (EF-TRK-03) ---
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

    /**
     * Détermine le type d'anomalie principal pour le champ typeAnomalie.
     */
    private String typeAnomaliePrincipal(AnomalieDetector.ResultatDetection detection) {
        if (detection.absenceProlongee()) return "ABSENCE_PROLONGEE";
        if (detection.arretProlonge()) return "ARRET_PROLONGE";
        if (detection.positionAberrante()) return "POSITION_ABERRANTE";
        if (detection.ecartCorridor()) return "ECART_CORRIDOR";
        return "INCONNU";
    }
}
