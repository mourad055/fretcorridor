package com.flysoft.fretcorridor.cap.web;

import com.flysoft.fretcorridor.cap.domain.CategorieMarchandise;
import com.flysoft.fretcorridor.cap.domain.TarifChauffeur;
import com.flysoft.fretcorridor.cap.domain.TarifChauffeurService;
import com.flysoft.fretcorridor.cap.security.JwtService;
import com.flysoft.fretcorridor.cap.web.dto.IntervalleTarifResponse;
import com.flysoft.fretcorridor.cap.web.dto.TarifChauffeurCreationRequest;
import com.flysoft.fretcorridor.cap.web.dto.TarifChauffeurModificationRequest;
import com.flysoft.fretcorridor.cap.web.dto.TarifChauffeurResponse;
import com.flysoft.fretcorridor.cap.web.dto.TarifDetailResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Tarification par chauffeur (plan-fretcorridor-reorientation.md §1bis,
 * spec-tarification-par-chauffeur.md). Deux familles d'endpoints :
 *  - "Mes tarifs" (JWT chauffeur) : CRUD de ses propres tarifs, meme
 *    pattern que CapaciteController.
 *  - Lecture interne (X-Internal-Service-Key) : consommee par
 *    service-opt (detail, un candidat a la fois au L1) et par le
 *    gateway/service-mkt pour l'intervalle affiche au client.
 */
@RestController
@RequestMapping("/api/cap/tarifs")
public class TarifChauffeurController {

    private final TarifChauffeurService tarifChauffeurService;
    private final JwtService jwtService;
    private final String cleInterneAttendue;

    public TarifChauffeurController(TarifChauffeurService tarifChauffeurService, JwtService jwtService,
                                     @Value("${fretcorridor.internal.service-key}") String cleInterneAttendue) {
        this.tarifChauffeurService = tarifChauffeurService;
        this.jwtService = jwtService;
        this.cleInterneAttendue = cleInterneAttendue;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TarifChauffeurResponse configurer(@Valid @RequestBody TarifChauffeurCreationRequest requete,
                                              @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        UUID transporteurId = jwtService.extraireActeurId(token);
        String tenantId = jwtService.extraireTenantId(token);
        TarifChauffeur tarif = tarifChauffeurService.configurer(
                transporteurId, tenantId, requete.axeId(), requete.categorieMarchandise(), requete.tarifFcfaParKg());
        return TarifChauffeurResponse.from(tarif);
    }

    @GetMapping("/mes")
    public List<TarifChauffeurResponse> mesTarifs(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        UUID transporteurId = jwtService.extraireActeurId(token);
        String tenantId = jwtService.extraireTenantId(token);
        return tarifChauffeurService.listerMesTarifs(transporteurId, tenantId).stream()
                .map(TarifChauffeurResponse::from)
                .toList();
    }

    @PatchMapping("/{id}")
    public TarifChauffeurResponse modifier(@PathVariable UUID id, @Valid @RequestBody TarifChauffeurModificationRequest requete,
                                            @RequestHeader("Authorization") String authHeader) {
        try {
            String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
            return TarifChauffeurResponse.from(tarifChauffeurService.modifierTarif(id, tenantId, requete.tarifFcfaParKg()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PatchMapping("/{id}/actif")
    public TarifChauffeurResponse toggleActif(@PathVariable UUID id, @RequestHeader("Authorization") String authHeader) {
        try {
            String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
            return TarifChauffeurResponse.from(tarifChauffeurService.toggleActif(id, tenantId));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirer(@PathVariable UUID id, @RequestHeader("Authorization") String authHeader) {
        try {
            String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
            tarifChauffeurService.supprimer(id, tenantId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // Lecture interne (OPT, L1) : un candidat precis. 404 si ce chauffeur
    // n'a pas configure/active de tarif pour cet axe+categorie -- OPT doit
    // alors l'exclure du calcul plutot que d'inventer un prix.
    @GetMapping("/detail")
    public TarifDetailResponse detail(@RequestParam UUID transporteurId, @RequestParam UUID axeId,
                                       @RequestParam CategorieMarchandise categorie,
                                       @RequestHeader(value = "X-Internal-Service-Key", required = false) String cleInterne) {
        if (!cleInterneAttendue.equals(cleInterne)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cle interne invalide ou absente");
        }
        return tarifChauffeurService.detailPourAppelInterne(transporteurId, axeId, categorie)
                .map(TarifDetailResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "aucun tarif actif pour ce chauffeur sur cet axe et cette categorie"));
    }

    // Intervalle affiche au client avant diffusion (gateway/service-mkt) --
    // pas de donnee personnelle exposee (juste min/max/compte), lecture
    // ouverte au meme titre que GET /api/geo/axes/actifs-matching cote
    // geo : l'authentification du client final reste du ressort du
    // gateway, pas de ce service interne.
    @GetMapping("/intervalle")
    public IntervalleTarifResponse intervalle(@RequestParam UUID axeId, @RequestParam CategorieMarchandise categorie) {
        return tarifChauffeurService.intervallePourAppelInterne(axeId, categorie)
                .map(IntervalleTarifResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "aucun chauffeur actif sur cet axe pour cette categorie"));
    }
}
