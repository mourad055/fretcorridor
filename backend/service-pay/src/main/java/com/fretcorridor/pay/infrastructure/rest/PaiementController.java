package com.fretcorridor.pay.infrastructure.rest;

import com.fretcorridor.pay.domain.*;
import com.fretcorridor.pay.infrastructure.prestataire.MockPrestatairePaiementAdapter;
import com.fretcorridor.pay.infrastructure.rest.dto.ClotureMissionRequest;
import com.fretcorridor.pay.infrastructure.rest.dto.EcritureResponse;
import com.fretcorridor.pay.infrastructure.rest.dto.GarantieResponse;
import com.fretcorridor.pay.infrastructure.rest.dto.ReversementRequest;
import com.fretcorridor.pay.infrastructure.rest.dto.SouscrireGarantieRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * FE-PAY-01/02 : orchestration du paiement (Sprint 8). L'ingestion par appel
 * REST direct (prise-en-charge/clôture) est un point d'entrée temporaire,
 * remplacé par une consommation d'événements Kafka
 * (MissionCloturee/AffectationConfirmee) une fois le bus câblé pour ce
 * service — cf. Plan d'Exécution §4.3.
 */
@RestController
@RequestMapping("/api/v1/pay")
public class PaiementController {

    private final GrandLivreService grandLivreService;
    private final SequestreService sequestreService;
    private final GarantieService garantieService;
    private final ReconciliationService reconciliationService;
    private final MockPrestatairePaiementAdapter prestataire;

    public PaiementController(
            GrandLivreService grandLivreService,
            SequestreService sequestreService,
            GarantieService garantieService,
            ReconciliationService reconciliationService,
            MockPrestatairePaiementAdapter prestataire
    ) {
        this.grandLivreService = grandLivreService;
        this.sequestreService = sequestreService;
        this.garantieService = garantieService;
        this.reconciliationService = reconciliationService;
        this.prestataire = prestataire;
    }

    @PostMapping("/missions/{missionId}/prise-en-charge")
    public ResponseEntity<Void> priseEnCharge(@PathVariable String missionId) {
        sequestreService.declencher(missionId);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/missions/{missionId}/cloture")
    public ResponseEntity<EcritureResponse> cloture(@PathVariable String missionId, @Valid @RequestBody ClotureMissionRequest request) {
        EcritureMiroir encaissement = grandLivreService.enregistrerEncaissement(
                request.tenantId(), missionId, request.montant(), request.referencePrestataire(), request.modePaiement());
        prestataire.confirmer(missionId, request.montant());
        sequestreService.liberer(missionId);
        return ResponseEntity.ok(EcritureResponse.from(encaissement));
    }

    /** EF-PAY-06 (terme contractuel) : souscrit la garantie tierce qui autorise la mission à être confirmée sans encaissement préalable. */
    @PostMapping("/missions/{missionId}/garantie")
    public ResponseEntity<GarantieResponse> souscrireGarantie(@PathVariable String missionId, @Valid @RequestBody SouscrireGarantieRequest request) {
        Garantie garantie = garantieService.souscrire(request.tenantId(), missionId, request.garantId(), request.montant(), request.referenceGarantie());
        return ResponseEntity.status(201).body(GarantieResponse.from(garantie));
    }

    @PostMapping("/missions/{missionId}/reversement")
    public ResponseEntity<EcritureResponse> reversement(@PathVariable String missionId, @Valid @RequestBody ReversementRequest request) {
        EcritureMiroir reversement = grandLivreService.enregistrerReversement(
                request.tenantId(), missionId, request.transporteurId(), request.montant(), request.referencePrestataire());
        return ResponseEntity.ok(EcritureResponse.from(reversement));
    }

    @GetMapping("/transporteurs/{transporteurId}/ecritures")
    public List<EcritureResponse> ecrituresTransporteur(@PathVariable String transporteurId) {
        return grandLivreService.ecrituresDuBeneficiaire(transporteurId).stream().map(EcritureResponse::from).toList();
    }

    @GetMapping("/tenants/{tenantId}/rapport")
    public List<EcritureResponse> rapportTenant(@PathVariable String tenantId) {
        return grandLivreService.ecrituresDuTenant(tenantId).stream().map(EcritureResponse::from).toList();
    }

    @PostMapping("/missions/{missionId}/reconciliation")
    public AlerteReconciliation reconcilier(@PathVariable String missionId) {
        return reconciliationService.reconcilier(missionId);
    }
}
