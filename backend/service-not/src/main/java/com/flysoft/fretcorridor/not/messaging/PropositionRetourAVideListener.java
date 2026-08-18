package com.flysoft.fretcorridor.not.messaging;

import com.flysoft.fretcorridor.not.client.ServiceCapClient;
import com.flysoft.fretcorridor.not.entity.Notification;
import com.flysoft.fretcorridor.not.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consomme proposition-retour-a-vide (service-opt, Moteur) — S12, écran
 * "notification de mission retour, acceptation/refus" côté app
 * Chauffeur/Transporteur.
 *
 * tourneeId/affectationId mutuellement exclusifs (contrat) : testé
 * explicitement, jamais supposé qu'un seul des deux est toujours présent.
 * referenceId de la notification = celui des deux qui est renseigné.
 */
@Component
public class PropositionRetourAVideListener {

    private static final Logger log = LoggerFactory.getLogger(PropositionRetourAVideListener.class);

    // Même hypothèse mono-tenant que les autres consommateurs d'événements
    // Moteur (AffectationConfirmeeListener, service-exe) — aucun tenant
    // porté par l'événement à ce jour.
    private static final String TENANT_ID_PHASE1 = "00000000-0000-0000-0000-000000000001";

    private final ServiceCapClient serviceCapClient;
    private final NotificationService notificationService;

    public PropositionRetourAVideListener(ServiceCapClient serviceCapClient, NotificationService notificationService) {
        this.serviceCapClient = serviceCapClient;
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "proposition-retour-a-vide",
            containerFactory = "propositionRetourAVideKafkaListenerContainerFactory")
    public void ingerer(PropositionRetourAVideEvent event) {
        UUID referenceId = event.tourneeId() != null ? event.tourneeId() : event.affectationId();
        if (referenceId == null) {
            log.warn("PropositionRetourAVide sans tourneeId ni affectationId - eventId={}, evenement ignore",
                    event.eventId());
            return;
        }

        serviceCapClient.resoudreTransporteur(event.capaciteId()).ifPresentOrElse(
                transporteurId -> notificationService.creer(
                        transporteurId,
                        "Mission de retour disponible",
                        "Un retour à vide est proposé après votre livraison. Consultez les détails pour accepter ou refuser.",
                        Notification.TypeNotification.PROPOSITION_RETOUR,
                        referenceId,
                        TENANT_ID_PHASE1),
                () -> log.warn("PropositionRetourAVide non notifiee (transporteur non resolu) - "
                        + "capacite={}, eventId={}", event.capaciteId(), event.eventId())
        );
    }
}
