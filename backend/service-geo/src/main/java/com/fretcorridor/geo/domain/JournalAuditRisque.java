package com.fretcorridor.geo.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Trace d'une decision de renseignement/deverrouillage du risque securitaire
 * d'un axe (G3, CDC S4.5 ; EF-GEO-04, S9.9). Entite officielle du modele CDC
 * S13 (JournalAudit) : "Trace inviolable des actions sensibles" - append-only,
 * jamais modifiee ni supprimee une fois creee (aucun setter apres persistance).
 */
@Entity
@Table(name = "journal_audit_risque", schema = "geo")
public class JournalAuditRisque {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "axe_id", nullable = false)
    private UUID axeId;

    @Column(name = "acteur_id", nullable = false)
    private UUID acteurId;

    @Column(name = "niveau_risque_avant")
    private String niveauRisqueAvant;

    @Column(name = "niveau_risque_apres", nullable = false)
    private String niveauRisqueApres;

    @Column(nullable = false, columnDefinition = "text")
    private String motif;

    @Column(name = "date_decision", nullable = false, updatable = false)
    private Instant dateDecision;

    protected JournalAuditRisque() {
        // Requis par JPA.
    }

    public JournalAuditRisque(UUID axeId, UUID acteurId, String niveauRisqueAvant,
                               String niveauRisqueApres, String motif) {
        this.axeId = axeId;
        this.acteurId = acteurId;
        this.niveauRisqueAvant = niveauRisqueAvant;
        this.niveauRisqueApres = niveauRisqueApres;
        this.motif = motif;
    }

    @PrePersist
    void onCreate() {
        this.dateDecision = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getAxeId() { return axeId; }
    public UUID getActeurId() { return acteurId; }
    public String getNiveauRisqueAvant() { return niveauRisqueAvant; }
    public String getNiveauRisqueApres() { return niveauRisqueApres; }
    public String getMotif() { return motif; }
    public Instant getDateDecision() { return dateDecision; }
}
