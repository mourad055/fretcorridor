package com.fretcorridor.bur.infrastructure.rest;

import com.fretcorridor.bur.domain.MissionAppparieeService;
import com.fretcorridor.bur.domain.ObservatoireService;
import com.fretcorridor.bur.infrastructure.rest.dto.DefinirEstimationMarcheRequest;
import com.fretcorridor.bur.infrastructure.rest.dto.MissionAppparieeResponse;
import com.fretcorridor.bur.infrastructure.rest.dto.ObservatoireAxeResponse;
import com.fretcorridor.bur.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Vue Bureau des missions appariées, matérialisée depuis Kafka
 * (AffectationConfirmeeListener) — jamais un appel direct à service-opt
 * (cf. MissionAppariee, Javadoc). Consommé par le gateway (EF-BUR-01/02),
 * pas directement par le navigateur.
 *
 * Bloquant audit CDC §Transverse (constat étendu, 20/08) : tenantId venait
 * jusqu'ici du query/corps sans vérification -- un acteur d'un tenant
 * pouvait consulter les missions/estimations d'un autre tenant en
 * atteignant ce port directement. tenantId (et acteurId pour
 * definirEstimationMarche) viennent désormais du JWT forwardé par le
 * gateway, même principe que DossierController côté service-adm.
 */
@RestController
@RequestMapping("/api/v1/bur")
public class MissionAppparieeController {

    private final MissionAppparieeService service;
    private final ObservatoireService observatoireService;
    private final JwtService jwtService;

    public MissionAppparieeController(MissionAppparieeService service, ObservatoireService observatoireService,
                                       JwtService jwtService) {
        this.service = service;
        this.observatoireService = observatoireService;
        this.jwtService = jwtService;
    }

    @GetMapping("/missions-appariees")
    public List<MissionAppparieeResponse> missionsAppariees(@RequestHeader("Authorization") String authHeader) {
        return service.listerParTenant(jwtService.extraireTenantId(authHeader.substring(7))).stream()
                .map(MissionAppparieeResponse::from).toList();
    }

    /** EF-BUR-03, UC-BUR-02 : indicateurs de marché d'un axe (volumes, prix médian et dispersion, déséquilibre directionnel). */
    @GetMapping("/observatoire")
    public ObservatoireAxeResponse observatoire(@RequestParam UUID axeId,
                                                 @RequestHeader("Authorization") String authHeader) {
        String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
        return ObservatoireAxeResponse.from(observatoireService.indicateursPourAxe(tenantId, axeId, Instant.now()));
    }

    /** EF-BUR-05, RG-087 : estimation déclarative du volume mensuel réel du marché d'un axe, saisie par un agent Bureau. */
    @PutMapping("/estimation-marche")
    public ResponseEntity<Void> definirEstimationMarche(@Valid @RequestBody DefinirEstimationMarcheRequest request,
                                                          @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        observatoireService.definirEstimationMarche(jwtService.extraireTenantId(token), request.axeId(),
                request.volumeMensuelEstime(), request.source(), jwtService.extraireActeurId(token), Instant.now());
        return ResponseEntity.noContent().build();
    }
}
