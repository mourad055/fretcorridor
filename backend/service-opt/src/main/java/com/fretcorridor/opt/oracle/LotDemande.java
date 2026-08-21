package com.fretcorridor.opt.oracle;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Colis individuel d'une demande (CDC S13, contrat demande-publiee-lots.yaml valide avec Mobile). */
@Entity
@Table(name = "lot_demande", schema = "opt")
public class LotDemande {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "demande_id", nullable = false)
    private UUID demandeId;

    @Column(name = "lot_id", nullable = false)
    private UUID lotId;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "type_catalogue", nullable = false, length = 100)
    private String typeCatalogue;

    @Column(nullable = false)
    private Integer quantite;

    @Column(name = "poids_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal poidsKg;

    @Column(name = "longueur_m") private Double longueurM;
    @Column(name = "largeur_m") private Double largeurM;
    @Column(name = "hauteur_m") private Double hauteurM;

    @Column(nullable = false) private boolean gerbable;
    @Column(nullable = false) private boolean fragile;

    @Column(name = "classe_danger", length = 50)
    private String classeDanger;

    @Column(name = "date_reception", nullable = false, updatable = false)
    private Instant dateReception;

    protected LotDemande() {
        // requis par JPA
    }

    public LotDemande(UUID demandeId, UUID lotId, UUID eventId, String typeCatalogue, Integer quantite,
                       BigDecimal poidsKg, Double longueurM, Double largeurM, Double hauteurM,
                       boolean gerbable, boolean fragile, String classeDanger) {
        this.demandeId = demandeId;
        this.lotId = lotId;
        this.eventId = eventId;
        this.typeCatalogue = typeCatalogue;
        this.quantite = quantite;
        this.poidsKg = poidsKg;
        this.longueurM = longueurM;
        this.largeurM = largeurM;
        this.hauteurM = hauteurM;
        this.gerbable = gerbable;
        this.fragile = fragile;
        this.classeDanger = classeDanger;
    }

    @PrePersist
    void onCreate() {
        this.dateReception = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDemandeId() { return demandeId; }
    public UUID getLotId() { return lotId; }
    public String getTypeCatalogue() { return typeCatalogue; }
    public Integer getQuantite() { return quantite; }
    public BigDecimal getPoidsKg() { return poidsKg; }
    // Dimensions (m) - consommees par OracleChargementService pour les
    // verifications volumiques/gabarit (EF-MAT-05/13). Null = donnee
    // manquante, verification sautee mais tracee, jamais devinee.
    public Double getLongueurM() { return longueurM; }
    public Double getLargeurM() { return largeurM; }
    public Double getHauteurM() { return hauteurM; }
    public boolean isGerbable() { return gerbable; }
    public boolean isFragile() { return fragile; }
    public String getClasseDanger() { return classeDanger; }
}
