package com.fretcorridor.adm.infrastructure.rest;

import com.fretcorridor.adm.domain.DecisionService;
import com.fretcorridor.adm.domain.Dossier;
import com.fretcorridor.adm.domain.EscaladeService;
import com.fretcorridor.adm.domain.FileTravailService;
import com.fretcorridor.adm.infrastructure.rest.dto.DecisionRequest;
import com.fretcorridor.adm.infrastructure.rest.dto.DossierResponse;
import com.fretcorridor.adm.infrastructure.rest.dto.OuvrirDossierRequest;
import com.fretcorridor.adm.infrastructure.rest.dto.OuvrirRecoursRequest;
import com.fretcorridor.adm.infrastructure.rest.dto.PriseEnChargeRequest;
import com.fretcorridor.adm.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * FE-ADM-01/02 : file de travail priorisée et dossier consolidé (Sprint 10).
 * L'ouverture par appel REST direct est un point d'entrée temporaire, en
 * attendant la consommation d'événements Kafka (LitigeSignale/IncidentDetecte)
 * une fois le bus câblé pour ce service — cf. Plan d'Exécution §4.3.
 */
@RestController
@RequestMapping("/api/v1/dossiers")
public class DossierController {

    private final FileTravailService fileTravailService;
    private final DecisionService decisionService;
    private final EscaladeService escaladeService;
    private final JwtService jwtService;

    public DossierController(FileTravailService fileTravailService, DecisionService decisionService,
                              EscaladeService escaladeService, JwtService jwtService) {
        this.fileTravailService = fileTravailService;
        this.decisionService = decisionService;
        this.escaladeService = escaladeService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<DossierResponse> ouvrir(@Valid @RequestBody OuvrirDossierRequest request) {
        Dossier dossier = fileTravailService.ouvrir(request.tenantId(), request.type(), request.priorite(),
                request.missionId(), request.parties() == null ? List.of() : request.parties(),
                request.preuvesReferences() == null ? List.of() : request.preuvesReferences(),
                request.delaiTraitement());
        return ResponseEntity.status(201).body(DossierResponse.from(dossier));
    }

    @GetMapping
    public List<DossierResponse> fileDeTravail(@RequestParam String tenantId) {
        return fileTravailService.lister(tenantId).stream().map(DossierResponse::from).toList();
    }

    @GetMapping("/{dossierId}")
    public DossierResponse parId(@PathVariable String dossierId,
                                  @RequestHeader("Authorization") String authHeader) {
        String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
        return DossierResponse.from(fileTravailService.consulter(dossierId, tenantId));
    }

    @PostMapping("/{dossierId}/prise-en-charge")
    public DossierResponse priseEnCharge(@PathVariable String dossierId,
                                          @Valid @RequestBody PriseEnChargeRequest request) {
        return DossierResponse.from(fileTravailService.prendreEnCharge(dossierId, request.acteurId()));
    }

    @PostMapping("/{dossierId}/decision")
    public DossierResponse decider(@PathVariable String dossierId, @Valid @RequestBody DecisionRequest request) {
        return DossierResponse.from(
                decisionService.trancher(dossierId, request.decision(), request.motif(), request.acteurId()));
    }

    /** EF-ADM-04/RG-098 : ouvre un recours contre une décision rendue, second Dossier lié à l'original. */
    @PostMapping("/{dossierId}/recours")
    public ResponseEntity<DossierResponse> ouvrirRecours(@PathVariable String dossierId,
                                                           @Valid @RequestBody OuvrirRecoursRequest request) {
        Dossier recours = fileTravailService.ouvrirRecours(dossierId, request.priorite(), request.delaiTraitement());
        return ResponseEntity.status(201).body(DossierResponse.from(recours));
    }

    @PostMapping("/escalade")
    public List<DossierResponse> escalader() {
        return escaladeService.detecterEtEscalader(Instant.now()).stream().map(DossierResponse::from).toList();
    }
}
