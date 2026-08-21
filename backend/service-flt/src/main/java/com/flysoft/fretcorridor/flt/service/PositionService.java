package com.flysoft.fretcorridor.flt.service;

import com.flysoft.fretcorridor.flt.client.ServiceExeClient;
import com.flysoft.fretcorridor.flt.dto.PositionDto;
import com.flysoft.fretcorridor.flt.entity.Position;
import com.flysoft.fretcorridor.flt.messaging.FltEventPublisher;
import com.flysoft.fretcorridor.flt.messaging.PositionBruteEvent;
import com.flysoft.fretcorridor.flt.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PositionService {

    // FIX bloquant audit 21/08 : la valeur historique "MOBILE_CHAUFFEUR"
    // violait le contrat position-brute.yaml:58-60 (enum GPS_NATIF |
    // GPS_DEGRADE | MANUEL) et la contrainte chk_source_capture de
    // service-trk (V2__add_position.sql:26) - chaque position publiée était
    // rejetée à l'ingestion et loguée "doublon ignoré" (catch générique),
    // tuant tout le pipeline temps réel (ETA + anomalies). Défaut conforme.
    private static final String SOURCE_CAPTURE_DEFAUT = "GPS_NATIF";

    private final PositionRepository positionRepository;
    private final ServiceExeClient serviceExeClient;
    private final FltEventPublisher eventPublisher;

    // Appelée par l'app Chauffeur/Transporteur — persiste toujours la
    // position (lecture "dernière position" côté app Client, EF-TRK-01),
    // puis publie best-effort vers service-trk (ETA + détection d'anomalies)
    // - ferme le canal Kafka mort "position-brute" (audit §7.1).
    @Transactional
    public void enregistrer(PositionDto.EnvoyerRequest request, String tenantId, String authHeader) {
        Position position = Position.builder()
                .missionId(request.getMissionId())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .horodatage(request.getHorodatage())
                .tenantId(tenantId)
                .build();
        positionRepository.save(position);

        // Degradation gracieuse (ENF-DIS-04) : sans vehicule affecte resolu
        // (mission pas encore assignee cote service-exe, ou service-exe
        // injoignable), la position reste enregistree mais n'est pas publiee
        // vers le Moteur ce tour-ci.
        Optional<UUID> vehiculeId = serviceExeClient.resoudreVehicule(request.getMissionId(), authHeader);
        vehiculeId.ifPresent(id -> eventPublisher.publierPositionBrute(new PositionBruteEvent(
                // eventId du contrat : celui généré par l'app À LA CAPTURE si
                // fourni (déduplication des ré-envois hors ligne côté trk,
                // ENF-SEC-03), sinon généré ici - comportement historique.
                request.getEventId() != null ? request.getEventId() : UUID.randomUUID(),
                request.getMissionId(),
                id,
                request.getLatitude(),
                request.getLongitude(),
                request.getSourceCapture() != null ? request.getSourceCapture() : SOURCE_CAPTURE_DEFAUT,
                request.getPrecisionMetres(),
                request.getHorodatage().atZone(ZoneId.systemDefault()).toInstant(),
                Instant.now()
        )));
    }

    // Consommée par l'app Client (S6) — peut renvoyer "absent" tant qu'aucun
    // chauffeur n'envoie de position pour cette mission.
    @Transactional(readOnly = true)
    public Optional<PositionDto.DernierePositionResponse> getDernierePosition(UUID missionId, String tenantId) {
        return positionRepository.findFirstByMissionIdAndTenantIdOrderByHorodatageDesc(missionId, tenantId)
                .map(PositionDto.DernierePositionResponse::fromEntity);
    }
}
