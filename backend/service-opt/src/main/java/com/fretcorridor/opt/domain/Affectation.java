package com.fretcorridor.opt.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Affectation persistee apres L1 (EF-MAT-01/02/03) - source de verite interne
 * au perimetre Moteur pour origine/destination/itineraire/tarification d'une
 * mission. Son id devient le "mission_id" : c'est ce que TRK consommera en
 * synchrone interne (AffectationController.consulter ci-apres) pour calculer
 * son ETA, comble le trou d'architecture identifie avant persistance.
 *
 * N'est jamais cree pour une demande sans capacite affectee (cf
 * AffectationL1Service : capaciteId == null => rien a persister, ce n'est
 * pas un mode degrade, juste une absence d'affectation ce cycle).
 *
 * Immuable comme Hub/Axe : pas de setters, toute evolution future (ex.
 * replanification Phase 2, EF-MAT-09) passera par une methode metier
 * explicite plutot qu'un setter generique.
 */
@Entity
@Table(name = "affectation", schema = "opt")
public class Affectation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "demande_id", nullable = false)
    private UUID demandeId;

    @Column(name = "capacite_id", nullable = false)
    private UUID capaciteId;

    @Column(name = "cycle_matching_id")
    private UUID cycleMatchingId;

    @Column(name = "origine_latitude", nullable = false)
    private double origineLatitude;

    @Column(name = "origine_longitude", nullable = false)
    private double origineLongitude;

    @Column(name = "destination_latitude", nullable = false)
    private double destinationLatitude;

    @Column(name = "destination_longitude", nullable = false)
    private double destinationLongitude;

    // Nullable : itineraire en mode degrade possible meme avec affectation valide
    // (cf javadoc migration V2 et AffectationResultat - jamais confondu avec
    // "pas d'affectation").
    @Column(name = "distance_metres")
    private Double distanceMetres;

    @Column(name = "duree_secondes")
    private Double dureeSecondes;

    @Column(name = "intervalle_confiance_secondes")
    private Double intervalleConfianceSecondes;

    @Column(name = "geometrie_encodee", columnDefinition = "text")
    private String geometrieEncodee;

    @Column(name = "cout_total", nullable = false, precision = 12, scale = 4)
    private BigDecimal coutTotal;

    @Column(name = "bareme_id")
    private UUID baremeId;

    @Column(name = "bareme_version")
    private Integer baremeVersion;

    @Column(name = "regime", length = 50)
    private String regime;

    @Column(name = "cout_base", precision = 12, scale = 4)
    private BigDecimal coutBase;

    @Column(name = "cout_variable_poids_taxable", precision = 12, scale = 4)
    private BigDecimal coutVariablePoidsTaxable;

    @Column(name = "cout_services", precision = 12, scale = 4)
    private BigDecimal coutServices;

    @Column(name = "facteur_tension_applique", precision = 12, scale = 4)
    private BigDecimal facteurTensionApplique;

    @Column(name = "prix_transport_avant_plancher", precision = 12, scale = 4)
    private BigDecimal prixTransportAvantPlancher;

    @Column(name = "plancher_applique")
    private Boolean plancherApplique;

    @Column(name = "prix_transport", precision = 12, scale = 4)
    private BigDecimal prixTransport;

    @Column(name = "commission_plateforme", precision = 12, scale = 4)
    private BigDecimal commissionPlateforme;

    @Column(name = "montant_verse_transporteur", precision = 12, scale = 4)
    private BigDecimal montantVerseTransporteur;

    @Column(name = "tarification_mode_degrade", nullable = false)
    private boolean tarificationModeDegrade;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    protected Affectation() {
        // Requis par JPA.
    }

    // Constructeur complet : c'est AffectationL1Service qui l'utilise, au
    // moment ou une affectation valide (capaciteId != null) sort du solveur
    // Kuhn-Munkres - jamais construit ailleurs.
    public Affectation(UUID demandeId, UUID capaciteId, UUID cycleMatchingId,
                        double origineLatitude, double origineLongitude,
                        double destinationLatitude, double destinationLongitude,
                        Double distanceMetres, Double dureeSecondes,
                        Double intervalleConfianceSecondes, String geometrieEncodee,
                        BigDecimal coutTotal,
                        UUID baremeId, Integer baremeVersion, String regime,
                        BigDecimal coutBase, BigDecimal coutVariablePoidsTaxable,
                        BigDecimal coutServices, BigDecimal facteurTensionApplique,
                        BigDecimal prixTransportAvantPlancher, Boolean plancherApplique,
                        BigDecimal prixTransport, BigDecimal commissionPlateforme,
                        BigDecimal montantVerseTransporteur, boolean tarificationModeDegrade) {
        this.demandeId = demandeId;
        this.capaciteId = capaciteId;
        this.cycleMatchingId = cycleMatchingId;
        this.origineLatitude = origineLatitude;
        this.origineLongitude = origineLongitude;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.distanceMetres = distanceMetres;
        this.dureeSecondes = dureeSecondes;
        this.intervalleConfianceSecondes = intervalleConfianceSecondes;
        this.geometrieEncodee = geometrieEncodee;
        this.coutTotal = coutTotal;
        this.baremeId = baremeId;
        this.baremeVersion = baremeVersion;
        this.regime = regime;
        this.coutBase = coutBase;
        this.coutVariablePoidsTaxable = coutVariablePoidsTaxable;
        this.coutServices = coutServices;
        this.facteurTensionApplique = facteurTensionApplique;
        this.prixTransportAvantPlancher = prixTransportAvantPlancher;
        this.plancherApplique = plancherApplique;
        this.prixTransport = prixTransport;
        this.commissionPlateforme = commissionPlateforme;
        this.montantVerseTransporteur = montantVerseTransporteur;
        this.tarificationModeDegrade = tarificationModeDegrade;
    }

    @PrePersist
    void onCreate() {
        this.dateCreation = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDemandeId() { return demandeId; }
    public UUID getCapaciteId() { return capaciteId; }
    public UUID getCycleMatchingId() { return cycleMatchingId; }
    public double getOrigineLatitude() { return origineLatitude; }
    public double getOrigineLongitude() { return origineLongitude; }
    public double getDestinationLatitude() { return destinationLatitude; }
    public double getDestinationLongitude() { return destinationLongitude; }
    public Double getDistanceMetres() { return distanceMetres; }
    public Double getDureeSecondes() { return dureeSecondes; }
    public Double getIntervalleConfianceSecondes() { return intervalleConfianceSecondes; }
    public String getGeometrieEncodee() { return geometrieEncodee; }
    public BigDecimal getCoutTotal() { return coutTotal; }
    public UUID getBaremeId() { return baremeId; }
    public Integer getBaremeVersion() { return baremeVersion; }
    public String getRegime() { return regime; }
    public BigDecimal getCoutBase() { return coutBase; }
    public BigDecimal getCoutVariablePoidsTaxable() { return coutVariablePoidsTaxable; }
    public BigDecimal getCoutServices() { return coutServices; }
    public BigDecimal getFacteurTensionApplique() { return facteurTensionApplique; }
    public BigDecimal getPrixTransportAvantPlancher() { return prixTransportAvantPlancher; }
    public Boolean getPlancherApplique() { return plancherApplique; }
    public BigDecimal getPrixTransport() { return prixTransport; }
    public BigDecimal getCommissionPlateforme() { return commissionPlateforme; }
    public BigDecimal getMontantVerseTransporteur() { return montantVerseTransporteur; }
    public boolean isTarificationModeDegrade() { return tarificationModeDegrade; }
    public Instant getDateCreation() { return dateCreation; }
}
