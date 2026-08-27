package com.fretcorridor.opt.domain;

import com.fretcorridor.dto.PointGeoDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Demande en attente d'un cycle de matching (EF-MAT-01, "par cycles a fenetre"). */
@Entity
@Table(name = "demande_en_attente", schema = "opt")
public class DemandeEnAttente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "demande_id", nullable = false)
    private UUID demandeId;

    @Column(name = "axe_id", nullable = false)
    private UUID axeId;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valeurs_criteres", nullable = false, columnDefinition = "jsonb")
    private Map<String, Double> valeursCriteres;

    @Column(name = "origine_latitude")
    private Double origineLatitude;

    @Column(name = "origine_longitude")
    private Double origineLongitude;

    @Column(name = "destination_latitude")
    private Double destinationLatitude;

    @Column(name = "destination_longitude")
    private Double destinationLongitude;

    @Column(name = "poids_taxable_kg")
    private BigDecimal poidsTaxableKg;

    // Sprint 12 (EF-MAT-06/RG-107) : nullable tant que Mobile ne publie pas
    // encore ce champ (cf DemandePublieeEvent javadoc) - mode permissif.
    @Column(name = "fenetre_debut")
    private Instant fenetreDebut;

    @Column(name = "fenetre_fin")
    private Instant fenetreFin;

    @Column(name = "type_emballage_nom")
    private String typeEmballageNom;

    private Integer quantite;

    @Column(name = "destinataire_nom")
    private String destinataireNom;

    @Column(name = "destinataire_telephone")
    private String destinataireTelephone;

    @Column(name = "mode_collecte")
    private String modeCollecte;

    @Column(name = "type_disponibilite")
    private String typeDisponibilite;

    @Column(name = "poids_total_kg")
    private Double poidsTotalKg;

    @Column(name = "grande_valeur")
    private Boolean grandeValeur;

    @Column(nullable = false)
    private boolean traitee = false;

    @Column(name = "date_reception", nullable = false, updatable = false)
    private Instant dateReception;

    protected DemandeEnAttente() {
        // requis par JPA
    }

    public DemandeEnAttente(UUID demandeId, UUID axeId, UUID eventId, Map<String, Double> valeursCriteres,
                             PointGeoDto origine, PointGeoDto destination, BigDecimal poidsTaxableKg,
                             Instant fenetreDebut, Instant fenetreFin,
                             String typeEmballageNom, Integer quantite,
                             String destinataireNom, String destinataireTelephone,
                             String modeCollecte, String typeDisponibilite,
                             Double poidsTotalKg, Boolean grandeValeur) {
        this.demandeId = demandeId;
        this.axeId = axeId;
        this.eventId = eventId;
        this.valeursCriteres = valeursCriteres;
        if (origine != null) {
            this.origineLatitude = origine.latitude();
            this.origineLongitude = origine.longitude();
        }
        if (destination != null) {
            this.destinationLatitude = destination.latitude();
            this.destinationLongitude = destination.longitude();
        }
        this.poidsTaxableKg = poidsTaxableKg;
        this.fenetreDebut = fenetreDebut;
        this.fenetreFin = fenetreFin;
        this.typeEmballageNom = typeEmballageNom;
        this.quantite = quantite;
        this.destinataireNom = destinataireNom;
        this.destinataireTelephone = destinataireTelephone;
        this.modeCollecte = modeCollecte;
        this.typeDisponibilite = typeDisponibilite;
        this.poidsTotalKg = poidsTotalKg;
        this.grandeValeur = grandeValeur;
    }

    @PrePersist
    void onCreate() {
        this.dateReception = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDemandeId() { return demandeId; }
    public UUID getAxeId() { return axeId; }
    public Map<String, Double> getValeursCriteres() { return valeursCriteres; }
    public boolean isTraitee() { return traitee; }
    public void marquerTraitee() { this.traitee = true; }

    // UC-MAT-02 (CDC) : un refus ou une expiration de PropositionMission ne
    // doit jamais perdre silencieusement la demande -- meme classe de bug
    // que le double-decrement de capacite deja corrige (audit du 23 aout) :
    // une entree marquee traitee=true est exclue de tout cycle futur
    // (findByAxeIdAndTraiteeFalse). Remise en file pour le prochain cycle
    // (RG-045, traitement par lots) plutot qu'une re-tentative immediate sur
    // le meme candidat refuse.
    public void remettreEnAttente() { this.traitee = false; }
    public BigDecimal getPoidsTaxableKg() { return poidsTaxableKg; }
    public Instant getFenetreDebut() { return fenetreDebut; }
    public Instant getFenetreFin() { return fenetreFin; }
    public String getTypeEmballageNom() { return typeEmballageNom; }
    public Integer getQuantite() { return quantite; }
    public String getDestinataireNom() { return destinataireNom; }
    public String getDestinataireTelephone() { return destinataireTelephone; }
    public String getModeCollecte() { return modeCollecte; }
    public String getTypeDisponibilite() { return typeDisponibilite; }
    public Double getPoidsTotalKg() { return poidsTotalKg; }
    public Boolean getGrandeValeur() { return grandeValeur; }
    // RG-105 : age de la demande dans la file - conditionne son eligibilite
    // a la fenetre de traitement de l'axe (fenetre adaptative par axe).
    public Instant getDateReception() { return dateReception; }

    public PointGeoDto getOrigine() {
        return (origineLatitude == null || origineLongitude == null)
                ? null : new PointGeoDto(origineLatitude, origineLongitude);
    }

    public PointGeoDto getDestination() {
        return (destinationLatitude == null || destinationLongitude == null)
                ? null : new PointGeoDto(destinationLatitude, destinationLongitude);
    }
}
