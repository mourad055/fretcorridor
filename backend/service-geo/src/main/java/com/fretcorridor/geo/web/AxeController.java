package com.fretcorridor.geo.web;

import com.fretcorridor.geo.domain.Axe;
import com.fretcorridor.geo.domain.AxeRepository;
import com.fretcorridor.geo.domain.Hub;
import com.fretcorridor.geo.domain.HubRepository;
import com.fretcorridor.geo.domain.JournalAuditRisque;
import com.fretcorridor.geo.domain.JournalAuditRisqueRepository;
import com.fretcorridor.geo.domain.JournalAuditConvention;
import com.fretcorridor.geo.domain.JournalAuditConventionRepository;
import com.fretcorridor.geo.web.dto.ConventionRepartitionRequest;
import com.fretcorridor.geo.web.dto.AxeCreationRequest;
import com.fretcorridor.geo.web.dto.RisqueSecuritaireRequest;
import com.fretcorridor.geo.web.dto.AxeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints internes du referentiel Axe (EF-GEO-01/02/03).
 * Consomme par OPT (filtre L0, synchrone interne) et par les cartes Web/Mobile
 * en lecture seule via l'API exposee ici.
 *
 * ENF-MUL-01 : isolation stricte par tenant, filtree ICI en base via
 * AxeRepository.findByTenantId. Correction du 2026-08-09 suite a l'audit
 * gateway : GET /api/geo/axes?tenantId=... filtre reellement, plus jamais
 * relabellise a posteriori par un appelant (cf. RealGeoAdapter, gateway).
 */
@RestController
@RequestMapping("/api/geo/axes")
public class AxeController {

    // Tenant unique de la Phase 1 (BGFT, client-ancre) - meme identifiant que
    // celui applique par la migration V8 sur les donnees existantes. Utilise
    // comme defaut a la creation quand aucun tenantId n'est fourni, pour ne
    // jamais laisser une ligne orpheline sans tenant. String (pas UUID) :
    // c'est le format utilise partout ailleurs (JWT, gateway) pour ce champ.
    private static final String TENANT_PHASE1_PAR_DEFAUT = "tenant-bgft-douala";

    private final AxeRepository axeRepository;
    private final HubRepository hubRepository;
    private final JournalAuditRisqueRepository journalAuditRisqueRepository;
    private final JournalAuditConventionRepository journalAuditConventionRepository;

    public AxeController(AxeRepository axeRepository, HubRepository hubRepository,
                          JournalAuditRisqueRepository journalAuditRisqueRepository,
                          JournalAuditConventionRepository journalAuditConventionRepository) {
        this.axeRepository = axeRepository;
        this.hubRepository = hubRepository;
        this.journalAuditRisqueRepository = journalAuditRisqueRepository;
        this.journalAuditConventionRepository = journalAuditConventionRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AxeResponse creerAxe(@Valid @RequestBody AxeCreationRequest requete) {
        Hub hubOrigine = trouverHubOuLever(requete.hubOrigineId());
        Hub hubDestination = trouverHubOuLever(requete.hubDestinationId());

        if (hubOrigine.getId().equals(hubDestination.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "un axe doit relier deux hubs distincts");
        }

        String tenantId = requete.tenantId() != null ? requete.tenantId() : TENANT_PHASE1_PAR_DEFAUT;
        Axe axe = new Axe(requete.nom(), hubOrigine, hubDestination, tenantId);
        if (requete.visibiliteActive() != null) {
            axe.setVisibiliteActive(requete.visibiliteActive());
        }
        if (requete.matchingActif() != null) {
            axe.setMatchingActif(requete.matchingActif());
        }
        if (requete.paiementActif() != null) {
            axe.setPaiementActif(requete.paiementActif());
        }
        if (requete.parametres() != null) {
            axe.setParametres(requete.parametres());
        }

        return AxeResponse.from(axeRepository.save(axe));
    }

    /**
     * ENF-MUL-01 : si tenantId est fourni, filtre reellement en base
     * (AxeRepository.findByTenantId) - jamais un filtrage cote appelant.
     * Sans tenantId (retro-compatibilite OPT, appel interne synchrone qui
     * n'a pas de notion de tenant), retourne tout - comportement inchange
     * pour ne pas casser le filtre L0 d'OPT.
     */
    @GetMapping
    public List<AxeResponse> listerAxes(@RequestParam(required = false) String tenantId) {
        List<Axe> axes = tenantId != null
                ? axeRepository.findByTenantId(tenantId)
                : axeRepository.findAll();
        return axes.stream().map(AxeResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AxeResponse obtenirAxe(@PathVariable UUID id) {
        return axeRepository.findById(id)
                .map(AxeResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "axe introuvable : " + id));
    }

    /**
     * Axes actifs pour le matching (EF-GEO-03) — consomme par OPT.
     *
     * G3 (CDC S4.5) / EF-GEO-04 (S9.9) : meme verrou d'entree que
     * matchingActif (G2) - un axe GELE n'est JAMAIS retourne ici, quel que
     * soit son etat matchingActif. C'est ce filtre qui garantit l'indicateur
     * "operations en zone gelee = 0" : OPT ne voit jamais ces axes, donc ne
     * peut structurellement jamais y declencher de cycle.
     */
    @GetMapping("/actifs-matching")
    public List<AxeResponse> listerAxesActifsPourMatching() {
        return axeRepository.findByMatchingActifTrue().stream()
                .filter(this::estOperationnelPourMatching)
                .map(AxeResponse::from)
                .toList();
    }

    /**
     * Cle "risqueSecuritaire" absente = pas de restriction (meme principe
     * permissif que rayonAppariementKm/detourMaxDistanceKm : jamais un
     * defaut invente). niveauRisque=GELE = exclusion stricte, sans exception -
     * NORMAL et SURVEILLE restent operationnels (la surveillance renforce la
     * gouvernance des donnees, cf ENF-ZS cote TRK, mais ne bloque pas
     * l'operation elle-meme).
     */
    private boolean estOperationnelPourMatching(Axe axe) {
        Object risque = axe.getParametres().get("risqueSecuritaire");
        if (!(risque instanceof Map<?, ?> risqueMap)) {
            return true;
        }
        Object niveau = risqueMap.get("niveauRisque");
        return !"GELE".equals(niveau);
    }

    /**
     * Bascule un des trois etats d'activation, independamment des deux autres
     * (EF-GEO-03). "etat" doit valoir : visibilite | matching | paiement.
     */
    @PatchMapping("/{id}/etats/{etat}")
    public AxeResponse basculerEtat(@PathVariable UUID id,
                                     @PathVariable String etat,
                                     @RequestParam boolean actif) {
        Axe axe = axeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "axe introuvable : " + id));

        switch (etat) {
            case "visibilite" -> axe.setVisibiliteActive(actif);
            case "matching" -> axe.setMatchingActif(actif);
            case "paiement" -> axe.setPaiementActif(actif);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "etat inconnu : " + etat + " (attendu : visibilite, matching ou paiement)");
        }

        return AxeResponse.from(axeRepository.save(axe));
    }

    /**
     * G3 (CDC S4.5) / EF-GEO-04 (S9.9) : renseigne le niveau de risque
     * securitaire de l'axe, avec decision explicite tracee (JournalAuditRisque,
     * append-only). C'est CETTE ecriture qui constitue le "renseignement a
     * jour et decision explicite tracee" exige par G3 - sans elle, l'axe reste
     * GELE par defaut pour tout niveau autre que NORMAL (cf isOperationnel ci-dessous).
     *
     * @Transactional implicite via Spring Data - axe.save() et le nouvel
     * enregistrement d'audit doivent reussir ensemble ou pas du tout (jamais
     * un axe deverrouille sans trace, ni une trace sans effet reel sur l'axe).
     */
    @PatchMapping("/{id}/risque-securitaire")
    public AxeResponse renseignerRisqueSecuritaire(@PathVariable UUID id,
                                                     @Valid @RequestBody RisqueSecuritaireRequest requete) {
        Axe axe = axeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "axe introuvable : " + id));

        Map<String, Object> parametres = new java.util.HashMap<>(axe.getParametres());
        Object risqueExistant = parametres.get("risqueSecuritaire");
        String niveauAvant = (risqueExistant instanceof Map<?, ?> m && m.get("niveauRisque") != null)
                ? m.get("niveauRisque").toString() : null;

        Map<String, Object> risque = new java.util.HashMap<>();
        risque.put("niveauRisque", requete.niveauRisque());
        risque.put("renseigneParActeurId", requete.acteurId().toString());
        risque.put("dateRenseignement", java.time.Instant.now().toString());
        risque.put("motif", requete.motif());
        parametres.put("risqueSecuritaire", risque);
        axe.setParametres(parametres);

        journalAuditRisqueRepository.save(new JournalAuditRisque(
                id, requete.acteurId(), niveauAvant, requete.niveauRisque(), requete.motif()));

        return AxeResponse.from(axeRepository.save(axe));
    }

    /**
     * G4 (CDC S4.5) / EF-GEO-05, RG-052 (S9.9, S3.3 C2) : renseigne la cle
     * de repartition conventionnelle d'un axe transfrontalier, avec decision
     * explicite tracee (JournalAuditConvention, append-only) - meme patron
     * que renseignerRisqueSecuritaire (Sprint 15, G3).
     *
     * La somme des parts doit valoir 100 - verifiee ICI, jamais supposee
     * cote client (G4 : configuration auditee, pas seulement versionnee).
     */
    @PatchMapping("/{id}/convention-repartition")
    public AxeResponse renseignerConventionRepartition(@PathVariable UUID id,
                                                          @Valid @RequestBody ConventionRepartitionRequest requete) {
        Axe axe = axeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "axe introuvable : " + id));

        double somme = requete.partsPourcent().values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(somme - 100.0) > 0.01) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "la somme des parts doit valoir 100 (obtenu : " + somme + ") - RG-052");
        }

        Map<String, Object> parametres = new java.util.HashMap<>(axe.getParametres());
        Object conventionExistante = parametres.get("conventionRepartition");
        String codeAvant = (conventionExistante instanceof Map<?, ?> m && m.get("conventionCode") != null)
                ? m.get("conventionCode").toString() : null;

        Map<String, Object> convention = new java.util.HashMap<>();
        convention.put("conventionCode", requete.conventionCode());
        convention.put("partsPourcent", requete.partsPourcent());
        convention.put("renseigneParActeurId", requete.acteurId().toString());
        convention.put("dateRenseignement", java.time.Instant.now().toString());
        convention.put("motif", requete.motif());
        parametres.put("conventionRepartition", convention);
        axe.setParametres(parametres);

        journalAuditConventionRepository.save(new JournalAuditConvention(
                id, requete.acteurId(), codeAvant, requete.conventionCode(),
                Map.copyOf(requete.partsPourcent()), requete.motif()));

        return AxeResponse.from(axeRepository.save(axe));
    }

    private Hub trouverHubOuLever(UUID hubId) {
        return hubRepository.findById(hubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "hub introuvable : " + hubId));
    }
}
