package com.flysoft.fretcorridor.exe.controller;

import com.flysoft.fretcorridor.exe.dto.MissionDto;
import com.flysoft.fretcorridor.exe.entity.EtapeMission;
import com.flysoft.fretcorridor.exe.security.JwtService;
import com.flysoft.fretcorridor.exe.service.MissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;
    private final JwtService jwtService;

    // UC-EXE-01 côté Client — 204 si aucune mission encore créée (matching pas encore actif)
    @GetMapping("/demande/{demandeId}/chronologie")
    public ResponseEntity<?> getChronologie(
            @PathVariable UUID demandeId, @RequestHeader("Authorization") String authHeader) {
        try {
            String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
            return missionService.getChronologiePourDemande(demandeId, tenantId)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.noContent().build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // S7 côté Chauffeur/Transporteur — cf. AffectationConfirmeeListener :
    // reste vide tant que transporteurId n'est pas peuplé en amont
    // (écart documenté, service-opt).
    @GetMapping("/mes")
    public ResponseEntity<List<MissionDto.MissionResumeResponse>> mesMissions(
            @RequestHeader("Authorization") String authHeader) {
        UUID transporteurId = jwtService.extraireActeurId(authHeader.substring(7));
        String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
        return ResponseEntity.ok(missionService.listerMesMissions(transporteurId, tenantId));
    }

    // S11 : ordre planifié de la tournée (multi-étapes, LTL consolidé) à
    // laquelle appartient une ou plusieurs Missions du chauffeur connecté.
    @GetMapping("/tournees/{tourneeId}")
    public ResponseEntity<?> getTournee(
            @PathVariable UUID tourneeId, @RequestHeader("Authorization") String authHeader) {
        try {
            UUID transporteurId = jwtService.extraireActeurId(authHeader.substring(7));
            String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
            return ResponseEntity.ok(missionService.getTournee(tourneeId, transporteurId, tenantId));
        } catch (RuntimeException e) {
            if ("MISSION_INTROUVABLE".equals(e.getMessage()) || "ACCES_REFUSE".equals(e.getMessage())) {
                return ResponseEntity.status(404).build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{missionId}")
    public ResponseEntity<?> getMission(
            @PathVariable UUID missionId, @RequestHeader("Authorization") String authHeader) {
        try {
            UUID transporteurId = jwtService.extraireActeurId(authHeader.substring(7));
            String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
            return ResponseEntity.ok(missionService.getChronologie(missionId, transporteurId, tenantId));
        } catch (RuntimeException e) {
            if ("MISSION_INTROUVABLE".equals(e.getMessage()) || "ACCES_REFUSE".equals(e.getMessage())) {
                return ResponseEntity.status(404).build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // EF-EXE-02/04 : le chauffeur journalise EN_TRANSIT/INCIDENT (aucune
    // preuve exigée par EF-EXE-03 pour ces deux types). PRISE_EN_CHARGE/
    // LIVRAISON sont rejetées ici (PREUVE_MANQUANTE) -- passer par
    // POST .../etapes (multipart) ci-dessous.
    @PostMapping(value = "/{missionId}/etapes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ajouterEtape(
            @PathVariable UUID missionId, @Valid @RequestBody MissionDto.AjouterEtapeRequest requete,
            @RequestHeader("Authorization") String authHeader) {
        try {
            UUID transporteurId = jwtService.extraireActeurId(authHeader.substring(7));
            String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
            return ResponseEntity.ok(missionService.ajouterEtape(missionId, transporteurId, tenantId, requete));
        } catch (RuntimeException e) {
            return reponseErreur(e);
        }
    }

    // RG-070/EF-EXE-03 : prise en charge ou livraison, avec preuve minimale
    // obligatoire (une ou plusieurs photos + signature tactile du tiers).
    @PostMapping(value = "/{missionId}/etapes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> ajouterEtapeAvecPreuve(
            @PathVariable UUID missionId,
            @RequestParam EtapeMission.TypeEtape type,
            @RequestParam String libelle,
            @RequestParam(required = false) LocalDateTime horodatageCapture,
            @RequestParam(required = false) List<MultipartFile> photos,
            @RequestParam(required = false) MultipartFile signature,
            @RequestHeader("Authorization") String authHeader) {
        try {
            UUID transporteurId = jwtService.extraireActeurId(authHeader.substring(7));
            String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
            MissionDto.AjouterEtapeRequest requete = new MissionDto.AjouterEtapeRequest();
            requete.setType(type);
            requete.setLibelle(libelle);
            requete.setHorodatageCapture(horodatageCapture);
            return ResponseEntity.ok(
                    missionService.ajouterEtape(missionId, transporteurId, tenantId, requete, photos, signature));
        } catch (RuntimeException e) {
            return reponseErreur(e);
        }
    }

    private ResponseEntity<?> reponseErreur(RuntimeException e) {
        if ("MISSION_INTROUVABLE".equals(e.getMessage()) || "ACCES_REFUSE".equals(e.getMessage())) {
            return ResponseEntity.status(404).build();
        }
        // 400, pas 409 : le gateway (RealMissionExecutionAdapter.est400) mappe
        // déjà ce statut sur EtapeRefuseeException pour ETAPE_HORS_SEQUENCE --
        // PREUVE_MANQUANTE suit le même contrat d'erreur, pas un nouveau code.
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
