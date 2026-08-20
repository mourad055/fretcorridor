package com.fretcorridor.pay.infrastructure.messaging;

import com.fretcorridor.pay.domain.LitigeMission;
import com.fretcorridor.pay.domain.LitigeMissionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * EF-PAY-08 : consomme DossierLitige (service-adm) pour suspendre le
 * reversement automatique en cas de contestation ouverte. Naturellement
 * idempotent — LitigeMissionRepositoryAdapter.enregistrerSiPlusRecent ignore
 * tout événement dont l'horodatage n'est pas strictement postérieur à
 * l'état déjà connu pour la mission (même principe que PositionEtaListener,
 * ADR 0014).
 */
@Component
public class DossierLitigeListener {

    private static final Logger log = LoggerFactory.getLogger(DossierLitigeListener.class);

    private final LitigeMissionPort litigeMissionPort;

    public DossierLitigeListener(LitigeMissionPort litigeMissionPort) {
        this.litigeMissionPort = litigeMissionPort;
    }

    @KafkaListener(topics = "dossier-litige", containerFactory = "dossierLitigeKafkaListenerContainerFactory")
    public void ingerer(DossierLitigeEvent event) {
        litigeMissionPort.enregistrerSiPlusRecent(new LitigeMission(
                event.missionId(), event.tenantId(), event.actif(), event.horodatage()));
        log.debug("Litige mis à jour - mission={}, actif={}, eventId={}", event.missionId(), event.actif(), event.eventId());
    }
}
