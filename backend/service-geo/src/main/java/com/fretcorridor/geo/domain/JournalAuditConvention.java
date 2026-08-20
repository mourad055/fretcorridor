package com.fretcorridor.geo.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Trace d'une decision de renseignement de la cle de repartition
 * conventionnelle d'un axe transfrontalier (G4, CDC S4.5 ; EF-GEO-05/RG-052,
 * S9.9). Meme principe que JournalAuditRisque (Sprint 15, G3) : entite
 * officielle JournalAudit du modele CDC S13, append-only.
 */
@Entity
@Table(name = "journal_audit_convention", schema = "geo")
public class JournalAuditConvention {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "axe_id", nullable = false)
    private UUID axeId;

    @Column(name = "acteur_id", nullable = false)
    private UUID acteurId;

    @Column(name = "convention_code_avant")
    private String conventionCodeAvant;

    @Column(name = "convention_code_apres", nullable = false)
    private String conventionCodeApres;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parts_pourcent_apres", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> partsPourcentApres;

    @Column(nullable = false, columnDefinition = "text")
    private String motif;

    @Column(name = "date_decision", nullable = false, updatable = false)
    private Instant dateDecision;

    protected JournalAuditConvention() {
        // Requis par JPA.
    }

    public JournalAuditConvention(UUID axeId, UUID acteurId, String conventionCodeAvant,
                                   String conventionCodeApres, Map<String, Object> partsPourcentApres,
                                   String motif) {
        this.axeId = axeId;
        this.acteurId = acteurId;
        this.conventionCodeAvant = conventionCodeAvant;
        this.conventionCodeApres = conventionCodeApres;
        this.partsPourcentApres = partsPourcentApres;
        this.motif = motif;
    }

    @PrePersist
    void onCreate() {
        this.dateDecision = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getAxeId() { return axeId; }
    public UUID getActeurId() { return acteurId; }
    public String getConventionCodeAvant() { return conventionCodeAvant; }
    public String getConventionCodeApres() { return conventionCodeApres; }
    public Map<String, Object> getPartsPourcentApres() { return partsPourcentApres; }
    public String getMotif() { return motif; }
    public Instant getDateDecision() { return dateDecision; }
}
