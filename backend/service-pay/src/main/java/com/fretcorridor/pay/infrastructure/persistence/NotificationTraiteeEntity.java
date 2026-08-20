package com.fretcorridor.pay.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * EF-PAY-05 : trace des clés d'idempotence déjà traitées — persistée (pas en
 * mémoire) pour que l'idempotence survive un redémarrage du service.
 */
@Entity
@Table(name = "notifications_traitees")
public class NotificationTraiteeEntity {

    @Id
    @Column(name = "idempotence_key")
    private String idempotenceKey;

    @Column(name = "traite_le", nullable = false)
    private Instant traiteLe;

    protected NotificationTraiteeEntity() {
        // JPA
    }

    NotificationTraiteeEntity(String idempotenceKey, Instant traiteLe) {
        this.idempotenceKey = idempotenceKey;
        this.traiteLe = traiteLe;
    }
}
