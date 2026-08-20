package com.fretcorridor.pay.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** {@code mission_id} est unique : une ligne par mission, mise à jour en place — cf. LitigeMissionRepositoryAdapter. */
@Entity
@Table(name = "litiges_mission")
public class LitigeMissionEntity {

    @Id
    @Column(name = "mission_id")
    private String missionId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private boolean actif;

    @Column(nullable = false)
    private Instant horodatage;

    protected LitigeMissionEntity() {
        // JPA
    }

    public LitigeMissionEntity(String missionId, String tenantId, boolean actif, Instant horodatage) {
        this.missionId = missionId;
        this.tenantId = tenantId;
        this.actif = actif;
        this.horodatage = horodatage;
    }

    public String getMissionId() {
        return missionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public boolean isActif() {
        return actif;
    }

    public Instant getHorodatage() {
        return horodatage;
    }

    public void mettreAJour(boolean actif, Instant horodatage) {
        this.actif = actif;
        this.horodatage = horodatage;
    }
}
