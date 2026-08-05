package com.fretcorridor.pay.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * FE-PAY-04 / ENF-FIN-03 : la réconciliation quotidienne lève une alerte
 * bloquante sur tout écart, et l'écart doit être isolé (jamais laissé VALIDE
 * en silence).
 */
public class ReconciliationService {

    private final GrandLivrePort grandLivrePort;
    private final PrestatairePaiementPort prestatairePaiementPort;

    public ReconciliationService(GrandLivrePort grandLivrePort, PrestatairePaiementPort prestatairePaiementPort) {
        this.grandLivrePort = grandLivrePort;
        this.prestatairePaiementPort = prestatairePaiementPort;
    }

    public AlerteReconciliation reconcilier(String missionId) {
        List<EcritureMiroir> ecritures = grandLivrePort.parMission(missionId);
        BigDecimal totalLocal = soldeNet(ecritures);
        BigDecimal totalPrestataire = prestatairePaiementPort.obtenirReleve(missionId).montantTotal();
        BigDecimal ecart = totalLocal.subtract(totalPrestataire).abs();

        if (ecart.signum() != 0) {
            ecritures.stream()
                    .filter(e -> e.statut() == StatutEcriture.VALIDE)
                    .forEach(e -> grandLivrePort.suspendre(e.id()));
            return new AlerteReconciliation(missionId, ecart, Instant.now(), true);
        }

        return new AlerteReconciliation(missionId, BigDecimal.ZERO, Instant.now(), false);
    }

    private BigDecimal soldeNet(List<EcritureMiroir> ecritures) {
        return ecritures.stream()
                .filter(e -> e.statut() == StatutEcriture.VALIDE)
                .map(e -> e.sens() == SensEcriture.CREDIT ? e.montant() : e.montant().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
