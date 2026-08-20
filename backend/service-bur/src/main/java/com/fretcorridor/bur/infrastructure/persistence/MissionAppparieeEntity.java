package com.fretcorridor.bur.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code eventId} est unique en base : c'est ce qui rend
 * {@link com.fretcorridor.bur.infrastructure.messaging.AffectationConfirmeeListener}
 * idempotent (un rejeu Kafka du même événement ne crée jamais de doublon —
 * même principe que DemandePublieeListener côté service-opt).
 */
@Entity
@Table(name = "missions_appariees", uniqueConstraints = @UniqueConstraint(columnNames = "event_id"))
public class MissionAppparieeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "axe_id")
    private UUID axeId;

    @Column(name = "transporteur_id")
    private UUID transporteurId;

    @Column(name = "origine_nom")
    private String origineNom;

    @Column(name = "destination_nom")
    private String destinationNom;

    @Column(name = "prix_transport", precision = 12, scale = 4)
    private BigDecimal prixTransport;

    @Column(name = "devise")
    private String devise;

    @Column(name = "confirmee_le", nullable = false)
    private Instant confirmeeLe;

    protected MissionAppparieeEntity() {
        // JPA
    }

    public MissionAppparieeEntity(UUID eventId, UUID missionId, String tenantId, UUID axeId, UUID transporteurId,
                                   String origineNom, String destinationNom, BigDecimal prixTransport,
                                   String devise, Instant confirmeeLe) {
        this.eventId = eventId;
        this.missionId = missionId;
        this.tenantId = tenantId;
        this.axeId = axeId;
        this.transporteurId = transporteurId;
        this.origineNom = origineNom;
        this.destinationNom = destinationNom;
        this.prixTransport = prixTransport;
        this.devise = devise;
        this.confirmeeLe = confirmeeLe;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getMissionId() {
        return missionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getAxeId() {
        return axeId;
    }

    public UUID getTransporteurId() {
        return transporteurId;
    }

    public String getOrigineNom() {
        return origineNom;
    }

    public String getDestinationNom() {
        return destinationNom;
    }

    public BigDecimal getPrixTransport() {
        return prixTransport;
    }

    public String getDevise() {
        return devise;
    }

    public Instant getConfirmeeLe() {
        return confirmeeLe;
    }
}
