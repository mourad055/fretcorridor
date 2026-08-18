package com.fretcorridor.adm.infrastructure.messaging;

import com.fretcorridor.adm.domain.FileTravailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * EF-PAY-09, ENF-FIN-03 : consomme EcartReconciliation (service-pay) pour
 * ouvrir un incident dans la file de travail Admin (EF-ADM-01 range déjà "la
 * modération, les incidents et les litiges" — un écart de réconciliation en
 * est un). Une fois ouvert, l'incident suit le régime commun de tout
 * Dossier : s'il dépasse son délai plafond sans être traité,
 * EscaladeScheduler (EF-ADM-05) l'escalade automatiquement — pas besoin de
 * réinventer une surveillance dédiée.
 */
@Component
public class EcartReconciliationListener {

    private static final Logger log = LoggerFactory.getLogger(EcartReconciliationListener.class);

    private final FileTravailService fileTravailService;
    private final Duration delaiTraitement;

    public EcartReconciliationListener(
            FileTravailService fileTravailService,
            @Value("${fretcorridor.adm.incident-reconciliation.delai-heures:48}") long delaiTraitementHeures
    ) {
        this.fileTravailService = fileTravailService;
        this.delaiTraitement = Duration.ofHours(delaiTraitementHeures);
    }

    @KafkaListener(topics = "ecart-reconciliation", containerFactory = "ecartReconciliationKafkaListenerContainerFactory")
    public void ingerer(EcartReconciliationEvent event) {
        String description = "Écart de réconciliation : " + event.ecart() + " (détecté le " + event.declencheeLe() + ")";
        var incident = fileTravailService.ouvrirIncidentReconciliation(
                event.tenantId(), event.missionId(), description, Instant.now().plus(delaiTraitement));
        if (incident.isPresent()) {
            log.warn("Incident réconciliation ouvert - mission={}, ecart={}, dossier={}",
                    event.missionId(), event.ecart(), incident.get().id());
        } else {
            log.debug("Incident réconciliation déjà ouvert - mission={}, eventId={}", event.missionId(), event.eventId());
        }
    }
}
