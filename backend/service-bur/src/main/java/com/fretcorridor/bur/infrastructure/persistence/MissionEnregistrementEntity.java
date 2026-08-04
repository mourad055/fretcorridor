package com.fretcorridor.bur.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "missions_enregistrees")
public class MissionEnregistrementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "axe_id", nullable = false)
    private String axeId;

    @Column(name = "enregistre_le", nullable = false)
    private Instant enregistreLe;

    protected MissionEnregistrementEntity() {
        // JPA
    }

    public MissionEnregistrementEntity(String tenantId, String axeId, Instant enregistreLe) {
        this.tenantId = tenantId;
        this.axeId = axeId;
        this.enregistreLe = enregistreLe;
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getAxeId() {
        return axeId;
    }

    public Instant getEnregistreLe() {
        return enregistreLe;
    }
}
