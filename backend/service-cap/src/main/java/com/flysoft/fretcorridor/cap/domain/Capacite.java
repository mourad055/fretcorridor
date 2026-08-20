package com.flysoft.fretcorridor.cap.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Capacite declaree (CDC S13). EF-CAP-07 : capaciteResiduelleKg est
 * decremente de facon atomique/idempotente (cf CapaciteService), jamais
 * modifie ailleurs qu'a travers cette methode.
 */
@Entity
@Table(name = "capacite", schema = "cap")
public class Capacite {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "vehicule_id", nullable = false)
    private UUID vehiculeId;

    @Column(name = "axe_id", nullable = false)
    private UUID axeId;

    // ENF-MUL-01 (audit CDC du 19 aout, corrige) : le tenant du declarant,
    // extrait du JWT (CapaciteController), jamais du corps de requete.
    // Nullable en base (migration V3) - toute nouvelle declaration le
    // renseigne, contrainte applicative.
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    // Resolu via ServiceFltClient au moment de la declaration (best-effort,
    // ferme le bug S7 - transporteurId toujours null cote OPT). Nullable :
    // service-flt injoignable ou vehicule non enregistre ne bloque jamais
    // la declaration (ENF-DIS-04).
    @Column(name = "transporteur_id")
    private UUID transporteurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_declaration", nullable = false, length = 30)
    private ModeDeclaration modeDeclaration;

    @Column(name = "poids_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal poidsKg;

    @Column(name = "volume_m3", precision = 10, scale = 3)
    private BigDecimal volumeM3;

    @Column(name = "longueur_plancher_m", precision = 6, scale = 2)
    private BigDecimal longueurPlancherM;

    @Column(name = "poids_taxable_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal poidsTaxableKg;

    @Column(name = "capacite_residuelle_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal capaciteResiduelleKg;

    @Column(name = "origine_latitude", nullable = false)
    private double origineLatitude;

    @Column(name = "origine_longitude", nullable = false)
    private double origineLongitude;

    @Column(name = "type_vehicule", length = 50)
    private String typeVehicule;

    @Column(name = "profil_hauteur_m", precision = 5, scale = 2)
    private BigDecimal profilHauteurM;

    @Column(name = "profil_largeur_m", precision = 5, scale = 2)
    private BigDecimal profilLargeurM;

    @Column(name = "profil_longueur_m", precision = 5, scale = 2)
    private BigDecimal profilLongueurM;

    @Column(name = "profil_poids_max_t", precision = 6, scale = 2)
    private BigDecimal profilPoidsMaxT;

    @Column(name = "profil_charge_essieu_max_t", precision = 6, scale = 2)
    private BigDecimal profilChargeEssieuMaxT;

    @Column(name = "profil_nb_essieux")
    private Integer profilNbEssieux;

    @Column(name = "profil_matieres_dangereuses", nullable = false)
    private boolean profilMatieresDangereuses;

    @Column(name = "date_depart", nullable = false)
    private Instant dateDepart;

    @Column(nullable = false)
    private boolean expiree = false;

    @Column(nullable = false)
    private boolean publiee = false;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    // EF-CAP-07 : verrou optimiste, condition du decrement atomique -
    // toute mise a jour concurrente incoherente leve OptimisticLockException
    // plutot que d'ecraser silencieusement une valeur.
    @Version
    private Long version;

    protected Capacite() {
        // requis par JPA
    }

    public Capacite(UUID vehiculeId, UUID axeId, String tenantId, UUID transporteurId, ModeDeclaration modeDeclaration,
                     BigDecimal poidsKg, BigDecimal volumeM3, BigDecimal longueurPlancherM,
                     BigDecimal poidsTaxableKg, double origineLatitude, double origineLongitude,
                     String typeVehicule, BigDecimal profilHauteurM, BigDecimal profilLargeurM,
                     BigDecimal profilLongueurM, BigDecimal profilPoidsMaxT,
                     BigDecimal profilChargeEssieuMaxT, Integer profilNbEssieux,
                     boolean profilMatieresDangereuses, Instant dateDepart) {
        this.vehiculeId = vehiculeId;
        this.axeId = axeId;
        this.tenantId = tenantId;
        this.transporteurId = transporteurId;
        this.modeDeclaration = modeDeclaration;
        this.poidsKg = poidsKg;
        this.volumeM3 = volumeM3;
        this.longueurPlancherM = longueurPlancherM;
        this.poidsTaxableKg = poidsTaxableKg;
        this.capaciteResiduelleKg = poidsKg; // capacite pleine a la declaration
        this.origineLatitude = origineLatitude;
        this.origineLongitude = origineLongitude;
        this.typeVehicule = typeVehicule;
        this.profilHauteurM = profilHauteurM;
        this.profilLargeurM = profilLargeurM;
        this.profilLongueurM = profilLongueurM;
        this.profilPoidsMaxT = profilPoidsMaxT;
        this.profilChargeEssieuMaxT = profilChargeEssieuMaxT;
        this.profilNbEssieux = profilNbEssieux;
        this.profilMatieresDangereuses = profilMatieresDangereuses;
        this.dateDepart = dateDepart;
    }

    @PrePersist
    void onCreate() {
        this.dateCreation = Instant.now();
    }

    /** EF-CAP-07 : decrement en memoire, verrou optimiste applique a la persistance (save). */
    void decrementer(BigDecimal montantKg) {
        if (montantKg.compareTo(this.capaciteResiduelleKg) > 0) {
            throw new IllegalStateException(
                    "Decrement refuse : montant (" + montantKg + ") superieur a la capacite residuelle ("
                            + this.capaciteResiduelleKg + ") pour la capacite " + this.id);
        }
        this.capaciteResiduelleKg = this.capaciteResiduelleKg.subtract(montantKg);
    }

    void marquerExpiree() {
        this.expiree = true;
    }

    void marquerPubliee() {
        this.publiee = true;
    }

    public UUID getId() { return id; }
    public UUID getVehiculeId() { return vehiculeId; }
    public UUID getAxeId() { return axeId; }
    public String getTenantId() { return tenantId; }
    public UUID getTransporteurId() { return transporteurId; }
    public ModeDeclaration getModeDeclaration() { return modeDeclaration; }
    public BigDecimal getPoidsKg() { return poidsKg; }
    public BigDecimal getVolumeM3() { return volumeM3; }
    public BigDecimal getPoidsTaxableKg() { return poidsTaxableKg; }
    public BigDecimal getCapaciteResiduelleKg() { return capaciteResiduelleKg; }
    public double getOrigineLatitude() { return origineLatitude; }
    public double getOrigineLongitude() { return origineLongitude; }
    public String getTypeVehicule() { return typeVehicule; }
    public BigDecimal getProfilHauteurM() { return profilHauteurM; }
    public BigDecimal getProfilLargeurM() { return profilLargeurM; }
    public BigDecimal getProfilLongueurM() { return profilLongueurM; }
    public BigDecimal getProfilPoidsMaxT() { return profilPoidsMaxT; }
    public BigDecimal getProfilChargeEssieuMaxT() { return profilChargeEssieuMaxT; }
    public Integer getProfilNbEssieux() { return profilNbEssieux; }
    public boolean isProfilMatieresDangereuses() { return profilMatieresDangereuses; }
    public Instant getDateDepart() { return dateDepart; }
    public boolean isExpiree() { return expiree; }
    public boolean isPubliee() { return publiee; }
    public Instant getDateCreation() { return dateCreation; }
}
