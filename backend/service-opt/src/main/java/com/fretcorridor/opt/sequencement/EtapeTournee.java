package com.fretcorridor.opt.sequencement;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Un enlevement ou une livraison, a un rang precis dans une Tournee
 * (CDC S8.6.1) :
 *  - point 1 (appariement) : affectationId fixe la demande ET le vehicule
 *  - point 2 (precedence) : verifie par le solveur (rang enlevement < rang
 *    livraison pour la meme affectation), jamais en base
 *  - point 3 (capacite dynamique) : chargeApresEtapeKg, verifiee a chaque
 *    etat intermediaire, pas seulement en somme totale
 */
@Entity
@Table(name = "etape_tournee", schema = "opt")
public class EtapeTournee {

    public enum TypeEtape { ENLEVEMENT, LIVRAISON }
    public enum Etat { PLANIFIEE, EXECUTEE }

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tournee_id", nullable = false)
    private Tournee tournee;

    @Column(name = "affectation_id", nullable = false)
    private UUID affectationId;

    @Column(nullable = false)
    private Integer rang;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_etape", nullable = false, length = 20)
    private TypeEtape typeEtape;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Etat etat = Etat.PLANIFIEE;

    @Column(name = "charge_apres_etape_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal chargeApresEtapeKg;

    @Column(name = "detour_distance_metres")
    private Double detourDistanceMetres;

    @Column(name = "detour_duree_secondes")
    private Double detourDureeSecondes;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    protected EtapeTournee() {
        // requis par JPA
    }

    public EtapeTournee(Tournee tournee, UUID affectationId, int rang, TypeEtape typeEtape,
                         BigDecimal chargeApresEtapeKg) {
        this.tournee = tournee;
        this.affectationId = affectationId;
        this.rang = rang;
        this.typeEtape = typeEtape;
        this.chargeApresEtapeKg = chargeApresEtapeKg;
    }

    @PrePersist
    void onCreate() {
        this.dateCreation = Instant.now();
    }

    /**
     * EF-MAT-09 : figeage - une etape executee ne doit plus jamais etre
     * recalculee/deplacee.
     *
     * @return true si cette execution vient de faire passer la Tournee
     *         parente a TERMINEE (cf Tournee.marquerEnExecutionSiNecessaire) -
     *         propage tel quel, jamais recalcule ici.
     */
    public boolean marquerExecutee() {
        this.etat = Etat.EXECUTEE;
        return this.tournee.marquerEnExecutionSiNecessaire();
    }

    /** RG-056/RG-108/EF-MAT-10 : detour subi par la demande de cette etape, une fois calcule. */
    public void enregistrerDetour(double distanceMetres, double dureeSecondes) {
        this.detourDistanceMetres = distanceMetres;
        this.detourDureeSecondes = dureeSecondes;
    }

    public UUID getId() { return id; }
    public Tournee getTournee() { return tournee; }
    public UUID getAffectationId() { return affectationId; }
    public Integer getRang() { return rang; }
    public TypeEtape getTypeEtape() { return typeEtape; }
    public Etat getEtat() { return etat; }
    public BigDecimal getChargeApresEtapeKg() { return chargeApresEtapeKg; }
    public Double getDetourDistanceMetres() { return detourDistanceMetres; }
    public Double getDetourDureeSecondes() { return detourDureeSecondes; }
}
