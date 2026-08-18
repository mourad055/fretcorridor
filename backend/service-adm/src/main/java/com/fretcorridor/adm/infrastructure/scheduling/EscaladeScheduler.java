package com.fretcorridor.adm.infrastructure.scheduling;

import com.fretcorridor.adm.domain.Dossier;
import com.fretcorridor.adm.domain.EscaladeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * EF-ADM-05 : escalade automatiquement tout dossier dépassant son délai
 * plafond (CDC UC-ADM-01, RG-099). Remplace l'ancien fonctionnement "à la
 * demande" de la Phase 1 (endpoint `POST /api/v1/dossiers/escalade`, conservé
 * comme déclenchement manuel). Même patron que
 * {@code ReversementAutomatiqueScheduler} côté service-pay (EF-PAY-08,
 * Phase 2) : toute la logique métier vit dans {@link EscaladeService}
 * (domaine pur, déjà testé) — cette classe ne fait que la déclencher
 * périodiquement.
 */
@Component
public class EscaladeScheduler {

    private static final Logger log = LoggerFactory.getLogger(EscaladeScheduler.class);

    private final EscaladeService service;

    public EscaladeScheduler(EscaladeService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${fretcorridor.adm.ordonnanceur-escalade.intervalle-ms:900000}")
    public void executer() {
        List<Dossier> escalades = service.detecterEtEscalader(Instant.now());
        if (!escalades.isEmpty()) {
            log.info("Escalade automatique : {} dossier(s) escaladé(s)", escalades.size());
        }
    }
}
