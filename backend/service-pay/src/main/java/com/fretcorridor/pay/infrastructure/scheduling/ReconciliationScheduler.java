package com.fretcorridor.pay.infrastructure.scheduling;

import com.fretcorridor.pay.domain.AlerteReconciliation;
import com.fretcorridor.pay.domain.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * EF-PAY-02, RG-077 : rapprochement automatique quotidien entre le grand
 * livre et les relevés du prestataire. Même patron que
 * ReversementAutomatiqueScheduler — la logique métier vit entièrement dans
 * {@link ReconciliationService} (domaine pur), cette classe ne fait que la
 * déclencher périodiquement.
 */
@Component
public class ReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationScheduler.class);

    private final ReconciliationService service;

    public ReconciliationScheduler(ReconciliationService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${fretcorridor.pay.ordonnanceur-reconciliation.intervalle-ms:900000}")
    public void executer() {
        List<AlerteReconciliation> alertes = service.reconcilierMissionsActives();
        if (!alertes.isEmpty()) {
            log.warn("Réconciliation quotidienne : {} écart(s) bloquant(s) détecté(s)", alertes.size());
        }
    }
}
