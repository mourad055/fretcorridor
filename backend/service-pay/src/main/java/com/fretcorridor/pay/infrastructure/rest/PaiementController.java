package com.fretcorridor.pay.infrastructure.rest;

import com.fretcorridor.pay.domain.*;
import com.fretcorridor.pay.infrastructure.prestataire.MockPrestatairePaiementAdapter;
import com.fretcorridor.pay.infrastructure.rest.dto.ChoisirModePaiementRequest;
import com.fretcorridor.pay.infrastructure.rest.dto.ClotureMissionRequest;
import com.fretcorridor.pay.infrastructure.rest.dto.ConfirmationLivraisonRequest;
import com.fretcorridor.pay.infrastructure.rest.dto.DeclarationEspecesResponse;
import com.fretcorridor.pay.infrastructure.rest.dto.DeclarerPaiementEspecesRequest;
import com.fretcorridor.pay.infrastructure.rest.dto.EcritureResponse;
import com.fretcorridor.pay.infrastructure.rest.dto.GarantieResponse;
import com.fretcorridor.pay.infrastructure.rest.dto.ModePaiementChoisiResponse;
import com.fretcorridor.pay.infrastructure.rest.dto.ReversementRequest;
import com.fretcorridor.pay.infrastructure.rest.dto.SouscrireGarantieRequest;
import com.fretcorridor.pay.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * FE-PAY-01/02 : orchestration du paiement (Sprint 8). {@code prise-en-charge}
 * et {@code cloture} (encaissement inclus) restent des points d'entrée REST
 * temporaires, faute d'événement de cycle de vie mission couvrant
 * l'encaissement — cf. Plan d'Exécution §4.3. {@code confirmation-livraison}
 * (libération seule, RG-078) ne l'est plus : depuis
 * {@link com.fretcorridor.pay.infrastructure.messaging.MissionLivreeListener},
 * service-exe déclenche la libération du séquestre via l'événement Kafka
 * MissionLivree — cet endpoint REST reste disponible (usage manuel/ops),
 * mais n'a plus d'appelant réel dans le flux nominal.
 */
@RestController
@RequestMapping("/api/v1/pay")
public class PaiementController {

    private final GrandLivreService grandLivreService;
    private final SequestreService sequestreService;
    private final GarantieService garantieService;
    private final PaiementEspecesService paiementEspecesService;
    private final ReconciliationService reconciliationService;
    private final ModePaiementChoisiService modePaiementChoisiService;
    private final MockPrestatairePaiementAdapter prestataire;
    private final JwtService jwtService;

    public PaiementController(
            GrandLivreService grandLivreService,
            SequestreService sequestreService,
            GarantieService garantieService,
            PaiementEspecesService paiementEspecesService,
            ReconciliationService reconciliationService,
            ModePaiementChoisiService modePaiementChoisiService,
            MockPrestatairePaiementAdapter prestataire,
            JwtService jwtService
    ) {
        this.grandLivreService = grandLivreService;
        this.sequestreService = sequestreService;
        this.garantieService = garantieService;
        this.paiementEspecesService = paiementEspecesService;
        this.modePaiementChoisiService = modePaiementChoisiService;
        this.reconciliationService = reconciliationService;
        this.prestataire = prestataire;
        this.jwtService = jwtService;
    }

    // Bloquant audit CDC §Transverse ("tenantId lu du corps de requête") :
    // tenantId vient désormais toujours du JWT, jamais du corps envoyé par
    // le client - même principe que service-cap (CapaciteController) et
    // service-flt (VehiculeController).
    private String tenantIdDuJwt(String authHeader) {
        return jwtService.extraireTenantId(authHeader.substring(7));
    }

    private boolean estAdministration(String authHeader) {
        return jwtService.extraireRoles(authHeader.substring(7)).contains("ADMINISTRATION");
    }

    @PostMapping("/missions/{missionId}/prise-en-charge")
    public ResponseEntity<Void> priseEnCharge(@PathVariable String missionId) {
        sequestreService.declencher(missionId);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/missions/{missionId}/cloture")
    public ResponseEntity<EcritureResponse> cloture(@PathVariable String missionId, @Valid @RequestBody ClotureMissionRequest request,
                                                      @RequestHeader("Authorization") String authHeader) {
        String tenantId = tenantIdDuJwt(authHeader);
        EcritureMiroir encaissement = grandLivreService.enregistrerEncaissement(
                tenantId, missionId, request.montant(), request.referencePrestataire(), request.modePaiement());
        prestataire.confirmer(missionId, request.montant());
        sequestreService.liberer(missionId, tenantId, request.transporteurId(), request.preuveLivraisonReference());
        return ResponseEntity.ok(EcritureResponse.from(encaissement, grandLivreService.litigeActifPourMission(missionId)));
    }

    /**
     * RG-078 : libère le séquestre indépendamment d'un encaissement — requis
     * avant tout reversement adossé à une garantie tierce (EF-PAY-06 terme
     * contractuel), où {@code cloture()} n'est jamais appelé.
     */
    @PostMapping("/missions/{missionId}/confirmation-livraison")
    public ResponseEntity<Void> confirmerLivraison(@PathVariable String missionId, @Valid @RequestBody ConfirmationLivraisonRequest request,
                                                     @RequestHeader("Authorization") String authHeader) {
        sequestreService.liberer(missionId, tenantIdDuJwt(authHeader), request.transporteurId(), request.preuveLivraisonReference());
        return ResponseEntity.noContent().build();
    }

    /** EF-PAY-06 (terme contractuel) : souscrit la garantie tierce qui autorise la mission à être confirmée sans encaissement préalable. */
    @PostMapping("/missions/{missionId}/garantie")
    public ResponseEntity<GarantieResponse> souscrireGarantie(@PathVariable String missionId, @Valid @RequestBody SouscrireGarantieRequest request,
                                                                @RequestHeader("Authorization") String authHeader) {
        Garantie garantie = garantieService.souscrire(tenantIdDuJwt(authHeader), missionId, request.garantId(), request.montant(), request.referenceGarantie());
        return ResponseEntity.status(201).body(GarantieResponse.from(garantie));
    }

    /**
     * EF-PAY-06, Item B de docs/DEPENDANCES_MOBILE_PHASE4.md, CDC §7.6 UC-PAY-01
     * étape 2 : le chargeur choisit son moyen de paiement, avant tout
     * encaissement réel — distinct de {@link EcritureMiroir#modePaiement()}.
     */
    @PostMapping("/missions/{missionId}/moyen-paiement")
    public ResponseEntity<ModePaiementChoisiResponse> choisirModePaiement(@PathVariable String missionId, @Valid @RequestBody ChoisirModePaiementRequest request,
                                                                            @RequestHeader("Authorization") String authHeader) {
        ModePaiementChoisi choix = modePaiementChoisiService.choisir(tenantIdDuJwt(authHeader), missionId, request.modePaiement(), Instant.now());
        return ResponseEntity.status(201).body(ModePaiementChoisiResponse.from(choix));
    }

    /** Affichage Chauffeur (Item B) : le moyen de paiement choisi pour une mission, avant tout encaissement. */
    @GetMapping("/missions/{missionId}/moyen-paiement")
    public ResponseEntity<ModePaiementChoisiResponse> moyenPaiementChoisi(@PathVariable String missionId) {
        return modePaiementChoisiService.pour(missionId)
                .map(choix -> ResponseEntity.ok(ModePaiementChoisiResponse.from(choix)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** EF-PAY-07 (S) : déclare le paiement en espèces d'une mission — mode dégradé, sans séquestre ni garantie. */
    @PostMapping("/missions/{missionId}/paiement-especes")
    public ResponseEntity<DeclarationEspecesResponse> declarerPaiementEspeces(@PathVariable String missionId, @Valid @RequestBody DeclarerPaiementEspecesRequest request,
                                                                                @RequestHeader("Authorization") String authHeader) {
        DeclarationEspeces declaration = paiementEspecesService.declarer(tenantIdDuJwt(authHeader), missionId, request.montant());
        return ResponseEntity.status(201).body(DeclarationEspecesResponse.from(declaration));
    }

    // IDOR corrigé (audit CDC §Transverse) : un tenant ne consulte plus les
    // paiements espèces d'un autre tenant que le sien, sauf ADMINISTRATION
    // (consultation transverse légitime, même principe que la gateway
    // AdmPort/PaiementReadController côté rapport financier admin).
    @GetMapping("/tenants/{tenantId}/paiements-especes")
    public List<DeclarationEspecesResponse> paiementsEspecesTenant(@PathVariable String tenantId,
                                                                     @RequestHeader("Authorization") String authHeader) {
        verifierAccesTenant(tenantId, authHeader);
        return paiementEspecesService.paiementsDuTenant(tenantId).stream().map(DeclarationEspecesResponse::from).toList();
    }

    @PostMapping("/missions/{missionId}/reversement")
    public ResponseEntity<EcritureResponse> reversement(@PathVariable String missionId, @Valid @RequestBody ReversementRequest request,
                                                          @RequestHeader("Authorization") String authHeader) {
        EcritureMiroir reversement = grandLivreService.enregistrerReversement(
                tenantIdDuJwt(authHeader), missionId, request.transporteurId(), request.montant(), request.referencePrestataire());
        return ResponseEntity.ok(EcritureResponse.from(reversement, grandLivreService.litigeActifPourMission(missionId)));
    }

    // IDOR corrigé (audit CDC §Transverse) : un transporteur ne consulte plus
    // les écritures d'un autre transporteur que lui-même, sauf ADMINISTRATION.
    @GetMapping("/transporteurs/{transporteurId}/ecritures")
    public List<EcritureResponse> ecrituresTransporteur(@PathVariable String transporteurId,
                                                          @RequestHeader("Authorization") String authHeader) {
        if (!estAdministration(authHeader) && !transporteurId.equals(jwtService.extraireActeurId(authHeader.substring(7)))) {
            throw new AccesRefuseException();
        }
        return grandLivreService.ecrituresDuBeneficiaire(transporteurId).stream()
                .map(e -> EcritureResponse.from(e, grandLivreService.litigeActifPourMission(e.missionId())))
                .toList();
    }

    // IDOR corrigé (audit CDC §Transverse) : un tenant ne consulte plus le
    // rapport financier d'un autre tenant que le sien, sauf ADMINISTRATION.
    @GetMapping("/tenants/{tenantId}/rapport")
    public List<EcritureResponse> rapportTenant(@PathVariable String tenantId,
                                                 @RequestHeader("Authorization") String authHeader) {
        verifierAccesTenant(tenantId, authHeader);
        return grandLivreService.ecrituresDuTenant(tenantId).stream()
                .map(e -> EcritureResponse.from(e, grandLivreService.litigeActifPourMission(e.missionId())))
                .toList();
    }

    private void verifierAccesTenant(String tenantId, String authHeader) {
        if (!estAdministration(authHeader) && !tenantId.equals(tenantIdDuJwt(authHeader))) {
            throw new AccesRefuseException();
        }
    }

    @PostMapping("/missions/{missionId}/reconciliation")
    public AlerteReconciliation reconcilier(@PathVariable String missionId) {
        return reconciliationService.reconcilier(missionId);
    }
}
