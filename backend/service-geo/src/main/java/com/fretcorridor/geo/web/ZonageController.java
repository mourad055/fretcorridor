package com.fretcorridor.geo.web;

import com.fretcorridor.geo.domain.Hub;
import com.fretcorridor.geo.domain.HubRepository;
import com.fretcorridor.geo.domain.ZonageH3Service;
import com.fretcorridor.geo.web.dto.HubResponse;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de zonage H3 (EF-GEO-01 complement, Sprint 3).
 *
 * C'est ici que OPT viendra chercher, en synchrone interne (meme porteur, budget
 * L0 ~50ms), la liste des hubs proches d'un point donne - typiquement l'origine
 * d'une demande de transport publiee par service-mkt (Mobile, evenement Kafka
 * consomme en amont par OPT, hors perimetre de ce controller).
 */
@RestController
@RequestMapping("/api/geo/zonage")
@Validated
public class ZonageController {

    private final ZonageH3Service zonageH3Service;
    private final HubRepository hubRepository;

    public ZonageController(ZonageH3Service zonageH3Service, HubRepository hubRepository) {
        this.zonageH3Service = zonageH3Service;
        this.hubRepository = hubRepository;
    }

    /**
     * Calcule l'index H3 d'un point quelconque (pas necessairement un hub existant).
     * Utile pour OPT : convertir l'origine d'une demande en index H3 avant de
     * chercher les hubs voisins via /hubs-proches ci-dessous.
     */
    @GetMapping("/index")
    public String indexPourPoint(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double longitude) {
        return zonageH3Service.indexPourPoint(latitude, longitude);
    }

    /**
     * k-ring brut : la cellule et ses k anneaux de voisines, sans jointure aux hubs.
     * Expose surtout pour le debug/verification manuelle du zonage.
     */
    @GetMapping("/k-ring")
    public List<String> kRing(@RequestParam String indexH3,
                               // Plafond a 3 : suffisant pour le besoin metier (filtre L0
                               // d'OPT), evite tout calcul de voisinage demesure (deni de
                               // service applicatif) si un appelant envoie un k arbitraire.
                               @RequestParam(defaultValue = "1") @Min(0) @Max(3) int k) {
        return zonageH3Service.kRing(indexH3, k);
    }

    /**
     * Le vrai endpoint metier consomme par OPT (filtre L0) : tous les hubs presents
     * dans la cellule d'un point donne ou dans ses k anneaux de voisines.
     * Reduit l'espace de recherche avant l'affectation Kuhn-Munkres (L1).
     */
    @GetMapping("/hubs-proches")
    public List<HubResponse> hubsProches(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
            @RequestParam(defaultValue = "1") @Min(0) @Max(3) int k) {
        String indexCentral = zonageH3Service.indexPourPoint(latitude, longitude);
        List<String> cellulesVoisines = zonageH3Service.kRing(indexCentral, k);

        List<Hub> hubsTrouves = hubRepository.findByH3IndexIn(cellulesVoisines);
        return hubsTrouves.stream().map(HubResponse::from).toList();
    }
}
