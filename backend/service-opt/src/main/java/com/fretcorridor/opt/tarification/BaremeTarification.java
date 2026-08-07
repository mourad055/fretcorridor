package com.fretcorridor.opt.tarification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Version figee d'un bareme tarifaire (CDC S8.9). Meme principe que
 * ModelePonderation cote service-mat : immuable une fois cree, jamais
 * modifiee en place - une correction de tarif = une nouvelle version en
 * base, pour que tout prix historique reste reconstructible (RG-115,
 * explicabilite) meme apres evolution du bareme.
 *
 * RG-112 - AUCUN BAREME EN DUR : chaque valeur numerique ici vient de la
 * base, jamais d'une constante Java. Le CDC precise qu'aucun bareme
 * homologue n'a pu etre identifie sur ce marche (ni trafic interieur, ni
 * corridor) - le systeme doit rester agnostique et capable d'en adopter un
 * sans reecriture.
 *
 * axeId nullable = bareme par defaut (regime/tarifs generiques), utilise en
 * l'absence de bareme specifique a l'axe (cf EF-GEO-02, "chaque axe a ses
 * propres parametres de matching/tarification"). Un axe peut donc soit
 * heriter du defaut, soit avoir son propre bareme actif.
 */
@Entity
@Table(name = "bareme_tarification", schema = "opt")
public class BaremeTarification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "axe_id")
    private UUID axeId;

    /**
     * CDC S8.9.1 - COUT_BASE indexe aussi par type de vehicule, pas
     * seulement par axe. NULL = barème générique pour l'axe (repli en
     * cascade, cf TarificationL4Service.resoudreBareme). Chaine libre,
     * meme logique anti-bareme-en-dur que "regime".
     */
    @Column(name = "type_vehicule", length = 30)
    private String typeVehicule;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false)
    private boolean actif;

    /**
     * RG-102 - coexistence des regimes. "POIDS_TAXABLE" ou
     * "FORFAITAIRE_VEHICULE". Chaine libre plutot qu'enum Java : un futur
     * troisieme regime ne doit pas exiger de redeploiement (meme logique
     * anti-bareme-en-dur que code_critere cote service-mat).
     */
    @Column(nullable = false, length = 30)
    private String regime;

    /** Regime POIDS_TAXABLE : cout de base par kilometre parcouru. */
    @Column(name = "cout_base_par_km", precision = 12, scale = 4)
    private BigDecimal coutBaseParKm;

    /** Regime FORFAITAIRE_VEHICULE : socle fixe, indexe axe + type vehicule (S8.9.1). */
    @Column(name = "cout_socle_forfaitaire", precision = 12, scale = 4)
    private BigDecimal coutSocleForfaitaire;

    /** Regime POIDS_TAXABLE : part variable, cout unitaire x poids taxable. */
    @Column(name = "cout_unitaire_poids_taxable", precision = 12, scale = 4)
    private BigDecimal coutUnitairePoidsTaxable;

    /** RG-113 - plancher activable par axe, desactive par defaut. */
    @Column(name = "prix_plancher_actif", nullable = false)
    private boolean prixPlancherActif;

    @Column(name = "prix_plancher", precision = 12, scale = 4)
    private BigDecimal prixPlancher;

    /**
     * RG-114 - bornes du facteur de tension marche, en fraction (ex. -0.10
     * a +0.30 = -10% a +30%). Empeche un prix de tripler lors d'un pic -
     * legitime economiquement, destructeur commercialement, et un risque
     * politique direct pour une plateforme dependante d'une licence publique.
     */
    @Column(name = "tension_min_fraction", nullable = false, precision = 5, scale = 4)
    private BigDecimal tensionMinFraction;

    @Column(name = "tension_max_fraction", nullable = false, precision = 5, scale = 4)
    private BigDecimal tensionMaxFraction;

    /**
     * RG-116 - commission plateforme, en fraction du prix transport.
     * HYPOTHESE D'EQUIPE (a confirmer avec le porteur PAY, pas une
     * certitude du CDC) : la commission est prelevee sur le montant reverse
     * au transporteur, le prix affiche au chargeur est le prix transport
     * seul - cf TarificationResultat pour le detail de ce choix.
     */
    @Column(name = "commission_taux_fraction", nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionTauxFraction;

    @Column(length = 255)
    private String description;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    protected BaremeTarification() {
        // requis par JPA
    }

    public UUID getId() { return id; }
    public UUID getAxeId() { return axeId; }
    public String getTypeVehicule() { return typeVehicule; }
    public Integer getVersion() { return version; }
    public boolean isActif() { return actif; }
    public String getRegime() { return regime; }
    public BigDecimal getCoutBaseParKm() { return coutBaseParKm; }
    public BigDecimal getCoutSocleForfaitaire() { return coutSocleForfaitaire; }
    public BigDecimal getCoutUnitairePoidsTaxable() { return coutUnitairePoidsTaxable; }
    public boolean isPrixPlancherActif() { return prixPlancherActif; }
    public BigDecimal getPrixPlancher() { return prixPlancher; }
    public BigDecimal getTensionMinFraction() { return tensionMinFraction; }
    public BigDecimal getTensionMaxFraction() { return tensionMaxFraction; }
    public BigDecimal getCommissionTauxFraction() { return commissionTauxFraction; }
    public String getDescription() { return description; }
    public Instant getDateCreation() { return dateCreation; }
}
