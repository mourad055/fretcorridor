package com.fretcorridor.bur.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Une seule ligne active par (tenantId, axeId) — redéfinir écrase la précédente (cf. EstimationMarcheAxe, EF-BUR-05). */
@Entity
@Table(name = "bur_estimation_marche_axe")
@IdClass(EstimationMarcheAxeEntity.Cle.class)
public class EstimationMarcheAxeEntity {

    @Id
    @Column(name = "tenant_id")
    private String tenantId;

    @Id
    @Column(name = "axe_id")
    private UUID axeId;

    @Column(name = "volume_mensuel_estime", nullable = false, precision = 19, scale = 4)
    private BigDecimal volumeMensuelEstime;

    @Column(nullable = false)
    private String source;

    @Column(name = "definie_par_acteur_id", nullable = false)
    private String definieParActeurId;

    @Column(name = "definie_le", nullable = false)
    private Instant definieLe;

    protected EstimationMarcheAxeEntity() {
        // JPA
    }

    public EstimationMarcheAxeEntity(String tenantId, UUID axeId, BigDecimal volumeMensuelEstime, String source,
                                      String definieParActeurId, Instant definieLe) {
        this.tenantId = tenantId;
        this.axeId = axeId;
        this.volumeMensuelEstime = volumeMensuelEstime;
        this.source = source;
        this.definieParActeurId = definieParActeurId;
        this.definieLe = definieLe;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getAxeId() {
        return axeId;
    }

    public BigDecimal getVolumeMensuelEstime() {
        return volumeMensuelEstime;
    }

    public String getSource() {
        return source;
    }

    public String getDefinieParActeurId() {
        return definieParActeurId;
    }

    public Instant getDefinieLe() {
        return definieLe;
    }

    public static class Cle implements Serializable {
        private String tenantId;
        private UUID axeId;

        public Cle() {
        }

        public Cle(String tenantId, UUID axeId) {
            this.tenantId = tenantId;
            this.axeId = axeId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Cle cle)) return false;
            return Objects.equals(tenantId, cle.tenantId) && Objects.equals(axeId, cle.axeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tenantId, axeId);
        }
    }
}
