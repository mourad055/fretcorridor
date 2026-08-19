package com.fretcorridor.pay.infrastructure.scheduling;

import com.fretcorridor.pay.domain.EcritureMiroir;
import com.fretcorridor.pay.domain.ReversementAutomatiqueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * EF-PAY-08 : premier véritable ordonnanceur du périmètre (contrairement à
 * EscaladeService côté service-adm, resté "à la demande" en Phase 1 —
 * cf. docs/CONFORMITE_web-socle.md §3). La logique métier vit entièrement
 * dans {@link ReversementAutomatiqueService} (domaine pur, testable sans
 * Spring) ; cette classe ne fait que le déclencher périodiquement.
 */
@Component
public class ReversementAutomatiqueScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReversementAutomatiqueScheduler.class);

    private final ReversementAutomatiqueService service;

    public ReversementAutomatiqueScheduler(ReversementAutomatiqueService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${fretcorridor.pay.ordonnanceur-reversement.intervalle-ms:900000}")
    public void executer() {
        List<EcritureMiroir> reversements = service.detecterEtReverser(Instant.now());
        if (!reversements.isEmpty()) {
            log.info("Reversement automatique : {} mission(s) reversée(s)", reversements.size());
        }
    }
}
