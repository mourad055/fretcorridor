package com.fretcorridor.adm.infrastructure.rest;

import com.fretcorridor.adm.domain.DecisionService;
import com.fretcorridor.adm.domain.Dossier;
import com.fretcorridor.adm.domain.EscaladeService;
import com.fretcorridor.adm.domain.FileTravailService;
import com.fretcorridor.adm.domain.AccesRefuseException;
import com.fretcorridor.adm.infrastructure.rest.dto.DecisionRequest;
import com.fretcorridor.adm.infrastructure.rest.dto.DossierResponse;
import com.fretcorridor.adm.infrastructure.rest.dto.OuvrirDossierRequest;
import com.fretcorridor.adm.infrastructure.rest.dto.OuvrirRecoursRequest;
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

    // Delai de traitement par defaut (audit de suivi, 23 aout, S19) : un
    // chargeur qui signale un litige (Mobile, app_client) n'a aucune idee
    // d'un "delai de traitement" administratif - HYPOTHESE D'EQUIPE (a
    // valider, meme statut que RISQUE_AXE=SURVEILLE->0.5 cote service-opt) :
    // 72h, jamais invente comme une donnee certaine, applique uniquement en
    // repli quand l'appelant (typiquement ADM en interne) n'en fournit pas.
    private static final java.time.Duration DELAI_TRAITEMENT_DEFAUT = java.time.Duration.ofHours(72);

    @PostMapping
    public ResponseEntity<DossierResponse> ouvrir(@Valid @RequestBody OuvrirDossierRequest request,
                                                    @RequestHeader("Authorization") String authHeader) {
        String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
        Instant delaiTraitement = request.delaiTraitement() != null
                ? request.delaiTraitement()
                : Instant.now().plus(DELAI_TRAITEMENT_DEFAUT);
        Dossier dossier = fileTravailService.ouvrir(tenantId, request.type(), request.priorite(),
                request.missionId(), request.parties() == null ? List.of() : request.parties(),
                request.preuvesReferences() == null ? List.of() : request.preuvesReferences(),
                request.motif(), request.description(), delaiTraitement);
        return ResponseEntity.status(201).body(DossierResponse.from(dossier));
    }

    // IDOR corrigé (audit CDC §Transverse) : un tenant ne consulte plus la
    // file de travail d'un autre tenant que le sien, sauf ADMINISTRATION
    // (même principe que service-pay/PaiementController).
    @GetMapping
    public List<DossierResponse> fileDeTravail(@RequestParam String tenantId,
                                                @RequestHeader("Authorization") String authHeader) {
        if (!estAdministration(authHeader) && !tenantId.equals(jwtService.extraireTenantId(authHeader.substring(7)))) {
            throw new AccesRefuseException();
        }
        return fileTravailService.lister(tenantId).stream().map(DossierResponse::from).toList();
    }

    private boolean estAdministration(String authHeader) {
        return jwtService.extraireRoles(authHeader.substring(7)).contains("ADMINISTRATION");
    }

    @GetMapping("/{dossierId}")
    public DossierResponse parId(@PathVariable String dossierId,
                                  @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String tenantId = jwtService.extraireTenantId(token);
        String acteurId = jwtService.extraireActeurId(token);
        return DossierResponse.from(fileTravailService.consulter(dossierId, tenantId, acteurId));
    }

    @PostMapping("/{dossierId}/prise-en-charge")
    public DossierResponse priseEnCharge(@PathVariable String dossierId,
                                          @RequestHeader("Authorization") String authHeader) {
        String acteurId = jwtService.extraireActeurId(authHeader.substring(7));
        return DossierResponse.from(fileTravailService.prendreEnCharge(dossierId, acteurId));
    }

    @PostMapping("/{dossierId}/decision")
    public DossierResponse decider(@PathVariable String dossierId, @Valid @RequestBody DecisionRequest request,
                                    @RequestHeader("Authorization") String authHeader) {
        String acteurId = jwtService.extraireActeurId(authHeader.substring(7));
        return DossierResponse.from(
                decisionService.trancher(dossierId, request.decision(), request.motif(), acteurId));
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
