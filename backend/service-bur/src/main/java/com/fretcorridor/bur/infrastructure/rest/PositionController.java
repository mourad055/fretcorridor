package com.fretcorridor.bur.infrastructure.rest;

import com.fretcorridor.bur.domain.PositionService;
import com.fretcorridor.bur.infrastructure.persistence.MissionAppparieeEntity;
import com.fretcorridor.bur.infrastructure.persistence.MissionAppparieeJpaRepository;
import com.fretcorridor.bur.infrastructure.rest.dto.PositionResponse;
import com.fretcorridor.bur.infrastructure.security.JwtService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Vue Bureau des positions, matérialisée depuis Kafka (PositionEtaListener)
 * — jamais un appel direct à service-trk (qui n'expose d'ailleurs aucune API
 * REST). Consommé par le gateway (RG-043/ENF-PRF-02), pas directement par le
 * navigateur.
 *
 * Bloquant audit CDC §Transverse (constat étendu, 20/08) : tenantId venait
 * du query param sans vérification -- tenantId vient désormais du JWT
 * forwardé par le gateway, même principe que MissionAppparieeController.
 */
@RestController
@RequestMapping("/api/v1/bur")
public class PositionController {

    private final PositionService service;
    private final JwtService jwtService;
    private final MissionAppparieeJpaRepository missionRepository;

    public PositionController(PositionService service, JwtService jwtService,
                              MissionAppparieeJpaRepository missionRepository) {
        this.service = service;
        this.jwtService = jwtService;
        this.missionRepository = missionRepository;
    }

    @GetMapping("/positions")
    public List<PositionResponse> positions(@RequestHeader("Authorization") String authHeader) {
        String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
        Map<UUID, String> libellesParMission = missionRepository.findByTenantIdOrderByConfirmeeLeDesc(tenantId)
                .stream()
                .collect(Collectors.toMap(
                        MissionAppparieeEntity::getMissionId,
                        m -> m.getOrigineNom() + " → " + m.getDestinationNom(),
                        (a, b) -> a));
        return service.listerParTenant(tenantId).stream()
                .map(p -> PositionResponse.from(p, libellesParMission.getOrDefault(
                        p.missionId(), p.vehiculeId() != null ? p.vehiculeId().toString() : p.missionId().toString())))
                .toList();
    }
}
