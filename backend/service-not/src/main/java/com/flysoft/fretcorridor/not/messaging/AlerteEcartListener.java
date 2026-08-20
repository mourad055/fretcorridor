package com.flysoft.fretcorridor.not.messaging;

import com.flysoft.fretcorridor.not.client.ServiceFltClient;
import com.flysoft.fretcorridor.not.entity.Notification;
import com.flysoft.fretcorridor.not.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme alerte-ecart (service-trk, Moteur) — canal jusqu'ici totalement
 * mort (audit §7.1) : service-trk publiait déjà l'événement (détection
 * d'anomalies EF-TRK-03) mais aucun service ne le consommait, ni en Kafka ni
 * en HTTP. Cf shared-contracts/asyncapi/events/alerte-ecart.yaml :
 * "Consommé par service-not (Mobile) pour notification multicanal".
 *
 * vehiculeId n'est pas dans les champs "required" du contrat AsyncAPI —
 * sans lui, aucun destinataire n'est résolvable, l'alerte est journalisée
 * (WARN) mais aucune notification n'est créée plutôt que d'inventer un
 * destinataire.
 */
@Component
public class AlerteEcartListener {

    private static final Logger log = LoggerFactory.getLogger(AlerteEcartListener.class);

    private final ServiceFltClient serviceFltClient;
    private final NotificationService notificationService;
    // Même hypothèse mono-tenant Phase 1 que PropositionRetourAVideListener (ADR 0011).
    private final String tenantIdPhase1;

    public AlerteEcartListener(ServiceFltClient serviceFltClient, NotificationService notificationService,
                                @Value("${fretcorridor.not.tenant-id-phase1}") String tenantIdPhase1) {
        this.serviceFltClient = serviceFltClient;
        this.notificationService = notificationService;
        this.tenantIdPhase1 = tenantIdPhase1;
    }

    @KafkaListener(topics = "alerte-ecart", containerFactory = "alerteEcartKafkaListenerContainerFactory")
    public void ingerer(AlerteEcartEvent event) {
        if (event.vehiculeId() == null) {
            log.warn("AlerteEcart sans vehiculeId - aucun destinataire resolvable, notification non creee - "
                    + "mission={}, eventId={}", event.missionId(), event.eventId());
            return;
        }

        serviceFltClient.resoudreProprietaire(event.vehiculeId()).ifPresentOrElse(
                transporteurId -> notificationService.creer(
                        transporteurId,
                        "Anomalie de suivi détectée",
                        libelle(event.typeAnomalie()) + " — " + event.description(),
                        Notification.TypeNotification.ALERTE_ECART,
                        event.missionId(),
                        tenantIdPhase1),
                () -> log.warn("AlerteEcart non notifiee (proprietaire non resolu) - "
                        + "vehicule={}, eventId={}", event.vehiculeId(), event.eventId())
        );
    }

    private String libelle(String typeAnomalie) {
        return switch (typeAnomalie) {
            case "ABSENCE_PROLONGEE" -> "Absence prolongée de position";
            case "ARRET_PROLONGE" -> "Arrêt prolongé";
            case "POSITION_ABERRANTE" -> "Position aberrante";
            case "ECART_CORRIDOR" -> "Écart de corridor";
            default -> "Anomalie de suivi";
        };
    }
}
