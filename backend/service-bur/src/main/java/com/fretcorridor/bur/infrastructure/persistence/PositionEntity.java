package com.fretcorridor.bur.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code mission_id} est unique : une ligne par mission, mise à jour en
 * place à chaque nouvelle position (contrairement à missions_appariees, qui
 * est append-only) — cf. PositionRepositoryAdapter.
 */
@Entity
@Table(name = "positions", uniqueConstraints = @UniqueConstraint(columnNames = "mission_id"))
public class PositionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "vehicule_id")
    private UUID vehiculeId;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "captured_le", nullable = false)
    private Instant capturedLe;

    protected PositionEntity() {
        // JPA
    }

    public PositionEntity(UUID missionId, String tenantId, UUID vehiculeId,
                           double latitude, double longitude, Instant capturedLe) {
        this.missionId = missionId;
        this.tenantId = tenantId;
        this.vehiculeId = vehiculeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capturedLe = capturedLe;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMissionId() {
        return missionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getVehiculeId() {
        return vehiculeId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Instant getCapturedLe() {
        return capturedLe;
    }

    public void mettreAJour(UUID vehiculeId, double latitude, double longitude, Instant capturedLe) {
        this.vehiculeId = vehiculeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capturedLe = capturedLe;
    }
}
