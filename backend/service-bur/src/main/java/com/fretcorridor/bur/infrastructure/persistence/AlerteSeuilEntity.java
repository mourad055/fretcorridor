package com.fretcorridor.bur.infrastructure.persistence;

import com.fretcorridor.bur.domain.Comparateur;
import com.fretcorridor.bur.domain.IndicateurObservatoire;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bur_alerte_seuil")
public class AlerteSeuilEntity {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "axe_id", nullable = false)
    private UUID axeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IndicateurObservatoire indicateur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Comparateur comparateur;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal seuil;

    @Column(name = "cree_par_acteur_id", nullable = false)
    private String creeParActeurId;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    protected AlerteSeuilEntity() {
        // JPA
    }

    public AlerteSeuilEntity(String id, String tenantId, UUID axeId, IndicateurObservatoire indicateur,
                              Comparateur comparateur, BigDecimal seuil, String creeParActeurId, Instant creeLe) {
        this.id = id;
        this.tenantId = tenantId;
        this.axeId = axeId;
        this.indicateur = indicateur;
        this.comparateur = comparateur;
        this.seuil = seuil;
        this.creeParActeurId = creeParActeurId;
        this.creeLe = creeLe;
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getAxeId() {
        return axeId;
    }

    public IndicateurObservatoire getIndicateur() {
        return indicateur;
    }

    public Comparateur getComparateur() {
        return comparateur;
    }

    public BigDecimal getSeuil() {
        return seuil;
    }

    public String getCreeParActeurId() {
        return creeParActeurId;
    }

    public Instant getCreeLe() {
        return creeLe;
    }
}
