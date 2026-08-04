package com.fretcorridor.mat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Trace d'execution d'une decision de matching (EF-MAT-11/12).
 *
 * Aucune utilite fonctionnelle immediate visible - c'est precisement la table
 * que l'equipe pourrait etre tentee de proposer de supprimer "pour aller plus
 * vite" (cf Plan d'execution S3). Elle est la condition de toute amelioration
 * mesurable du moteur et de tout litige instruisible : sans elle, impossible
 * de reconstituer pourquoi une capacite plutot qu'une autre a ete retenue pour
 * une demande donnee.
 *
 * Pas de FK vers capacite_id/demande_id : ces entites vivent dans service-cap
 * et service-mkt (Mobile, autre porteur) - une FK inter-service coupterait le
 * deploiement de mat a celui de cap/mkt (cf Plan d'execution S4.1).
 */
@Entity
@Table(name = "cycle_matching", schema = "mat")
public class CycleMatching {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "capacite_id", nullable = false)
    private UUID capaciteId;

    @Column(name = "demande_id", nullable = false)
    private UUID demandeId;

    // Nullable uniquement en mode degrade (aucun modele actif trouve en base) -
    // trace explicitement qu'aucune version de ponderation n'a pu etre utilisee.
    @Column(name = "modele_ponderation_id")
    private UUID modelePonderationId;

    @Column(name = "cout_total", nullable = false, precision = 12, scale = 4)
    private BigDecimal coutTotal;

    // Detail de la contribution de chaque critere au cout total, pas seulement
    // le total - c'est ce qui rend la decision reconstructible (EF-MAT-11).
    // @JdbcTypeCode(SqlTypes.JSON) : mapping JSON natif Hibernate 6.x (bundle
    // avec Spring Boot 3.3.4), aucune dependance supplementaire necessaire.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details_couts", nullable = false, columnDefinition = "jsonb")
    private Map<String, Double> detailsCouts;

    @Column(name = "mode_degrade", nullable = false)
    private boolean modeDegrade;

    @Column(name = "date_execution", nullable = false, updatable = false)
    private Instant dateExecution;

    protected CycleMatching() {
        // requis par JPA
    }

    public CycleMatching(UUID capaciteId, UUID demandeId, UUID modelePonderationId,
                          BigDecimal coutTotal, Map<String, Double> detailsCouts,
                          boolean modeDegrade) {
        this.capaciteId = capaciteId;
        this.demandeId = demandeId;
        this.modelePonderationId = modelePonderationId;
        this.coutTotal = coutTotal;
        this.detailsCouts = detailsCouts;
        this.modeDegrade = modeDegrade;
    }

    @PrePersist
    void onCreate() {
        this.dateExecution = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCapaciteId() { return capaciteId; }
    public UUID getDemandeId() { return demandeId; }
    public UUID getModelePonderationId() { return modelePonderationId; }
    public BigDecimal getCoutTotal() { return coutTotal; }
    public Map<String, Double> getDetailsCouts() { return detailsCouts; }
    public boolean isModeDegrade() { return modeDegrade; }
    public Instant getDateExecution() { return dateExecution; }
}
