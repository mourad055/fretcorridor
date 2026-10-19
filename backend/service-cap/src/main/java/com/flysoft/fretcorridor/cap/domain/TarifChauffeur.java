package com.flysoft.fretcorridor.cap.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Tarif d'un chauffeur pour un axe et une categorie de marchandise
 * (plan-fretcorridor-reorientation.md §1bis) : F/kg a plat, fixe par le
 * chauffeur lui-meme. axeId reference geo.axe.id -- pas de FK cross-service,
 * geo est un autre porteur (Moteur).
 *
 * Source de verite pour le taux au kg desormais lue par
 * TarificationL4Service (service-opt) au lieu de
 * BaremeTarification.coutUnitairePoidsTaxable -- BaremeTarification reste
 * la source de la politique plateforme (commission, plancher, tension).
 */
@Entity
@Table(name = "tarif_chauffeur", schema = "cap")
public class TarifChauffeur {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "transporteur_id", nullable = false)
    private UUID transporteurId;

    @Column(name = "axe_id", nullable = false)
    private UUID axeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie_marchandise", nullable = false, length = 20)
    private CategorieMarchandise categorieMarchandise;

    @Column(name = "tarif_fcfa_par_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifFcfaParKg;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @Column(name = "date_mise_a_jour", nullable = false)
    private Instant dateMiseAJour;

    protected TarifChauffeur() {
        // requis par JPA
    }

    public TarifChauffeur(UUID transporteurId, UUID axeId, CategorieMarchandise categorieMarchandise,
                           BigDecimal tarifFcfaParKg, String tenantId) {
        this.transporteurId = transporteurId;
        this.axeId = axeId;
        this.categorieMarchandise = categorieMarchandise;
        this.tarifFcfaParKg = tarifFcfaParKg;
        this.tenantId = tenantId;
    }

    @PrePersist
    void onCreate() {
        this.dateCreation = Instant.now();
        this.dateMiseAJour = this.dateCreation;
    }

    @PreUpdate
    void onUpdate() {
        this.dateMiseAJour = Instant.now();
    }

    public void modifierTarif(BigDecimal nouveauTarif) {
        if (nouveauTarif == null || nouveauTarif.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le tarif doit etre strictement positif");
        }
        this.tarifFcfaParKg = nouveauTarif;
    }

    public void toggleActif() {
        this.actif = !this.actif;
    }

    public UUID getId() { return id; }
    public UUID getTransporteurId() { return transporteurId; }
    public UUID getAxeId() { return axeId; }
    public CategorieMarchandise getCategorieMarchandise() { return categorieMarchandise; }
    public BigDecimal getTarifFcfaParKg() { return tarifFcfaParKg; }
    public boolean isActif() { return actif; }
    public String getTenantId() { return tenantId; }
    public Instant getDateCreation() { return dateCreation; }
    public Instant getDateMiseAJour() { return dateMiseAJour; }
}
