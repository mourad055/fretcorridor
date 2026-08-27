package com.fretcorridor.gateway.domain.not;

import reactor.core.publisher.Flux;

/**
 * Port hexagonal : le domaine ignore que l'implémentation actuelle est un mock.
 * TODO(mobile): remplacer par l'appel réel à service-not une fois ce service
 * livré (Sprint 9 Mobile).
 */
public interface NotificationPort {

    /** ENF-MUL-01 : un Bureau ne voit que les notifications de son propre tenant. */
    Flux<Notification> listerNotificationsParTenant(String tenantId, String delegationToken);
}
