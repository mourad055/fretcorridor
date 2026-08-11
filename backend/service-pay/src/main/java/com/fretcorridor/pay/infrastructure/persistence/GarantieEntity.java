package com.fretcorridor.pay.infrastructure.persistence;

import com.fretcorridor.pay.domain.Garantie;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "garanties")
public class GarantieEntity {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "mission_id", nullable = false, unique = true)
    private String missionId;

    @Column(name = "garant_id", nullable = false)
    private String garantId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    @Column(name = "reference_garantie")
    private String referenceGarantie;

    @Column(name = "engagee_le", nullable = false)
    private Instant engageeLe;

    protected GarantieEntity() {
        // JPA
    }

    static GarantieEntity from(Garantie garantie) {
        GarantieEntity entity = new GarantieEntity();
        entity.id = garantie.id();
        entity.tenantId = garantie.tenantId();
        entity.missionId = garantie.missionId();
        entity.garantId = garantie.garantId();
        entity.montant = garantie.montant();
        entity.referenceGarantie = garantie.referenceGarantie();
        entity.engageeLe = garantie.engageeLe();
        return entity;
    }

    Garantie toDomain() {
        return new Garantie(id, tenantId, missionId, garantId, montant, referenceGarantie, engageeLe);
    }
}
