package com.fretcorridor.gateway.domain.not;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Notifications de l'acteur mobile connecté (S9) — appel réel à service-not,
 * séparé de NotificationPort (vue Bureau, encore mockée). Le push FCM
 * effectif (réception hors application) n'est pas câblé côté mobile — aucun
 * projet Firebase disponible dans ce dépôt ; seul le centre de notifications
 * "tiré" (liste + marquage lu) est réel.
 */
public interface NotificationMobilePort {
    Flux<NotificationMobile> mesNotifications(String delegationToken);

    Mono<Integer> nombreNonLues(String delegationToken);

    Mono<Void> marquerLue(String delegationToken, String notificationId);

    // S12 : acceptation/refus d'une proposition de retour à vide.
    Mono<Void> repondre(String delegationToken, String notificationId, boolean accepte);
}
