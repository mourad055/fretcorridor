package com.fretcorridor.gateway.infrastructure.rest.pay;

import com.fretcorridor.gateway.domain.adm.AdmPort;
import com.fretcorridor.gateway.domain.pay.PayReadPort;
import com.fretcorridor.gateway.infrastructure.rest.pay.dto.DeclarationEspecesVueResponse;
import com.fretcorridor.gateway.infrastructure.rest.pay.dto.EcritureVueResponse;
import com.fretcorridor.gateway.infrastructure.rest.pay.dto.SoldeTransporteurResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * FE-TRP-03 (solde/historique) + rapport financier Bureau/Admin (Sprint 8,
 * lecture seule). Consomme le service-pay réel via {@link PayReadPort} —
 * aucune écriture n'est possible depuis ces endpoints (lecture seule stricte,
 * cohérent avec ENF-FIN-01).
 */
@RestController
public class PaiementReadController {

    private final PayReadPort payReadPort;
    private final AdmPort admPort;

    public PaiementReadController(PayReadPort payReadPort, AdmPort admPort) {
        this.payReadPort = payReadPort;
        this.admPort = admPort;
    }

    @GetMapping("/api/v1/transporteur/paiement")
    public Mono<SoldeTransporteurResponse> soldeTransporteur(@AuthenticationPrincipal AuthenticatedActor actor) {
        return payReadPort.ecrituresDuTransporteur(actor.actorId())
                .map(EcritureVueResponse::from)
                .collectList()
                .map(historique -> new SoldeTransporteurResponse(
                        historique.stream().map(EcritureVueResponse::montant).reduce(BigDecimal.ZERO, BigDecimal::add),
                        historique
                ));
    }

    /** EF-BUR-06 : consultation de données individuelles (écritures nominatives) journalisée. */
    @GetMapping("/api/v1/bureau/rapport-financier")
    public Mono<java.util.List<EcritureVueResponse>> rapportFinancierBureau(@AuthenticationPrincipal AuthenticatedActor actor) {
        return admPort.enregistrerAudit(actor.tenantId(), actor.actorId(), "CONSULTATION_RAPPORT_FINANCIER", "tenant:" + actor.tenantId())
                .then(payReadPort.rapportDuTenant(actor.tenantId()).map(EcritureVueResponse::from).collectList());
    }

    @GetMapping("/api/v1/admin/rapport-financier/{tenantId}")
    public Mono<java.util.List<EcritureVueResponse>> rapportFinancierAdmin(
            @PathVariable String tenantId,
            @AuthenticationPrincipal AuthenticatedActor actor
    ) {
        // ENF-SEC-02 : consultation transverse d'un tenant par un Admin, journalisée nominativement.
        return admPort.enregistrerAudit(tenantId, actor.actorId(), "CONSULTATION_RAPPORT_FINANCIER", "tenant:" + tenantId)
                .then(payReadPort.rapportDuTenant(tenantId).map(EcritureVueResponse::from).collectList());
    }

    /** EF-PAY-07 (S) : missions payées en espèces (mode dégradé, sans protection) du territoire du Bureau. EF-BUR-06 : consultation journalisée. */
    @GetMapping("/api/v1/bureau/paiements-especes")
    public Mono<java.util.List<DeclarationEspecesVueResponse>> paiementsEspecesBureau(@AuthenticationPrincipal AuthenticatedActor actor) {
        return admPort.enregistrerAudit(actor.tenantId(), actor.actorId(), "CONSULTATION_PAIEMENTS_ESPECES", "tenant:" + actor.tenantId())
                .then(payReadPort.paiementsEspecesDuTenant(actor.tenantId()).map(DeclarationEspecesVueResponse::from).collectList());
    }

    @GetMapping("/api/v1/admin/paiements-especes/{tenantId}")
    public Mono<java.util.List<DeclarationEspecesVueResponse>> paiementsEspecesAdmin(
            @PathVariable String tenantId,
            @AuthenticationPrincipal AuthenticatedActor actor
    ) {
        // ENF-SEC-02 : consultation transverse d'un tenant par un Admin, journalisée nominativement.
        return admPort.enregistrerAudit(tenantId, actor.actorId(), "CONSULTATION_PAIEMENTS_ESPECES", "tenant:" + tenantId)
                .then(payReadPort.paiementsEspecesDuTenant(tenantId).map(DeclarationEspecesVueResponse::from).collectList());
    }
}
