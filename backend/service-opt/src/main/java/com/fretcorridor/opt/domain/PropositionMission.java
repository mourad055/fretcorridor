package com.fretcorridor.opt.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * UC-MAT-02 du CDC ("Notification, acceptation ou refus d'une mission par le
 * chauffeur", page 43) : le rang 1 du L1 devient une proposition EN_ATTENTE
 * notifiee au transporteur, plutot qu'une {@link Affectation} auto-confirmee
 * -- voir la javadoc de la migration V22 pour le detail de l'ecart au CDC
 * corrige ici. Rien de tarifaire duplique (voir V22) : reconstruit via
 * TarificationL4Service a l'acceptation, a partir de axeId/typeVehicule/
 * poidsTaxableKg/distanceMetres (deterministe).
 */
@Entity
@Table(name = "proposition_mission", schema = "opt")
public class PropositionMission {

    public enum Statut { EN_ATTENTE, ACCEPTEE, REFUSEE, EXPIREE }

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "demande_id", nullable = false)
    private UUID demandeId;

    @Column(name = "capacite_id", nullable = false)
    private UUID capaciteId;

    @Column(name = "transporteur_id", nullable = false)
    private UUID transporteurId;

    @Column(name = "vehicule_id")
    private UUID vehiculeId;

    @Column(name = "type_vehicule", length = 100)
    private String typeVehicule;

    @Column(name = "cycle_matching_id")
    private UUID cycleMatchingId;

    @Column(name = "axe_id")
    private UUID axeId;

    @Column(name = "rang", nullable = false)
    private int rang;

    @Column(name = "poids_taxable_kg", precision = 12, scale = 3)
    private BigDecimal poidsTaxableKg;

    @Column(name = "origine_nom")
    private String origineNom;

    @Column(name = "destination_nom")
    private String destinationNom;

    @Column(name = "origine_latitude", nullable = false)
    private double origineLatitude;

    @Column(name = "origine_longitude", nullable = false)
    private double origineLongitude;

    @Column(name = "destination_latitude", nullable = false)
    private double destinationLatitude;

    @Column(name = "destination_longitude", nullable = false)
    private double destinationLongitude;

    @Column(name = "distance_metres")
    private Double distanceMetres;

    @Column(name = "duree_secondes")
    private Double dureeSecondes;

    @Column(name = "intervalle_confiance_secondes")
    private Double intervalleConfianceSecondes;

    @Column(name = "geometrie_encodee", columnDefinition = "text")
    private String geometrieEncodee;

    @Column(name = "prix_transport", precision = 12, scale = 4)
    private BigDecimal prixTransport;

    // RG-048 (tracabilite des decisions) : reporte tel quel sur
    // opt.affectation.coutTotal a l'acceptation.
    @Column(name = "cout_total", nullable = false, precision = 12, scale = 4)
    private BigDecimal coutTotal;

    @Column(name = "type_emballage_nom")
    private String typeEmballageNom;

    @Column(name = "quantite")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private Statut statut = Statut.EN_ATTENTE;

    @Column(name = "motif_refus")
    private String motifRefus;

    @Column(name = "expire_a", nullable = false)
    private Instant expireA;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @Column(name = "date_reponse")
    private Instant dateReponse;

    protected PropositionMission() {
        // Requis par JPA.
    }

    public PropositionMission(UUID demandeId, UUID capaciteId, UUID transporteurId, UUID vehiculeId,
                               String typeVehicule, UUID cycleMatchingId, UUID axeId, int rang,
                               BigDecimal poidsTaxableKg, String origineNom, String destinationNom,
                               double origineLatitude, double origineLongitude,
                               double destinationLatitude, double destinationLongitude,
                               Double distanceMetres, Double dureeSecondes,
                               Double intervalleConfianceSecondes, String geometrieEncodee,
                               BigDecimal prixTransport, BigDecimal coutTotal,
                               String typeEmballageNom, Integer quantite,
                               String destinataireNom, String destinataireTelephone, String modeCollecte,
                               String typeDisponibilite, Double poidsTotalKg, Boolean grandeValeur,
                               Instant expireA) {
        this.demandeId = demandeId;
        this.capaciteId = capaciteId;
        this.transporteurId = transporteurId;
        this.vehiculeId = vehiculeId;
        this.typeVehicule = typeVehicule;
        this.cycleMatchingId = cycleMatchingId;
        this.axeId = axeId;
        this.rang = rang;
        this.poidsTaxableKg = poidsTaxableKg;
        this.origineNom = origineNom;
        this.destinationNom = destinationNom;
        this.origineLatitude = origineLatitude;
        this.origineLongitude = origineLongitude;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.distanceMetres = distanceMetres;
        this.dureeSecondes = dureeSecondes;
        this.intervalleConfianceSecondes = intervalleConfianceSecondes;
        this.geometrieEncodee = geometrieEncodee;
        this.prixTransport = prixTransport;
        this.coutTotal = coutTotal;
        this.typeEmballageNom = typeEmballageNom;
        this.quantite = quantite;
        this.destinataireNom = destinataireNom;
        this.destinataireTelephone = destinataireTelephone;
        this.modeCollecte = modeCollecte;
        this.typeDisponibilite = typeDisponibilite;
        this.poidsTotalKg = poidsTotalKg;
        this.grandeValeur = grandeValeur;
        this.expireA = expireA;
    }

    @PrePersist
    void onCreate() {
        this.dateCreation = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDemandeId() { return demandeId; }
    public UUID getCapaciteId() { return capaciteId; }
    public UUID getTransporteurId() { return transporteurId; }
    public UUID getVehiculeId() { return vehiculeId; }
    public String getTypeVehicule() { return typeVehicule; }
    public UUID getCycleMatchingId() { return cycleMatchingId; }
    public UUID getAxeId() { return axeId; }
    public int getRang() { return rang; }
    public BigDecimal getPoidsTaxableKg() { return poidsTaxableKg; }
    public String getOrigineNom() { return origineNom; }
    public String getDestinationNom() { return destinationNom; }
    public double getOrigineLatitude() { return origineLatitude; }
    public double getOrigineLongitude() { return origineLongitude; }
    public double getDestinationLatitude() { return destinationLatitude; }
    public double getDestinationLongitude() { return destinationLongitude; }
    public Double getDistanceMetres() { return distanceMetres; }
    public Double getDureeSecondes() { return dureeSecondes; }
    public Double getIntervalleConfianceSecondes() { return intervalleConfianceSecondes; }
    public String getGeometrieEncodee() { return geometrieEncodee; }
    public BigDecimal getPrixTransport() { return prixTransport; }
    public BigDecimal getCoutTotal() { return coutTotal; }
    public String getTypeEmballageNom() { return typeEmballageNom; }
    public Integer getQuantite() { return quantite; }
    public String getDestinataireNom() { return destinataireNom; }
    public String getDestinataireTelephone() { return destinataireTelephone; }
    public String getModeCollecte() { return modeCollecte; }
    public String getTypeDisponibilite() { return typeDisponibilite; }
    public Double getPoidsTotalKg() { return poidsTotalKg; }
    public Boolean getGrandeValeur() { return grandeValeur; }
    public Statut getStatut() { return statut; }
    public String getMotifRefus() { return motifRefus; }
    public Instant getExpireA() { return expireA; }
    public Instant getDateCreation() { return dateCreation; }
    public Instant getDateReponse() { return dateReponse; }

    public boolean estExpiree() {
        return statut == Statut.EN_ATTENTE && Instant.now().isAfter(expireA);
    }

    /**
     * RG-050 (3 interactions max) : transition uniquement depuis EN_ATTENTE
     * et non expiree -- protege aussi contre E3 (acceptation concurrente,
     * deux tentatives simultanees sur la meme proposition) puisque JPA
     * verrouille la ligne le temps de la transaction (@Transactional cote
     * service) et que ce garde-fou refuse toute transition hors EN_ATTENTE.
     *
     * @return true si CETTE invocation vient de faire la transition ; false
     *         si la proposition n'etait deja plus disponible (deja
     *         acceptee/refusee/expiree par une autre requete).
     */
    public boolean accepterSiPossible() {
        if (statut != Statut.EN_ATTENTE || estExpiree()) {
            return false;
        }
        this.statut = Statut.ACCEPTEE;
        this.dateReponse = Instant.now();
        return true;
    }

    public boolean refuserSiPossible(String motif) {
        if (statut != Statut.EN_ATTENTE || estExpiree()) {
            return false;
        }
        this.statut = Statut.REFUSEE;
        this.motifRefus = motif;
        this.dateReponse = Instant.now();
        return true;
    }

    /** RG-051 : le refus n'affecte pas l'indice de conformite -- appele en lecture (getMesPropositions) et en tâche de fond, jamais un refus. */
    public void marquerExpireeSiNecessaire() {
        if (statut == Statut.EN_ATTENTE && Instant.now().isAfter(expireA)) {
            this.statut = Statut.EXPIREE;
            this.dateReponse = Instant.now();
        }
    }
}
