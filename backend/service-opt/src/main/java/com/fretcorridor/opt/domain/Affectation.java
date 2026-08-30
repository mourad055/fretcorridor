package com.fretcorridor.opt.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Diffusion-course (plan de reorientation post-demo, remplace le modele CDC
 * "3 propositions classees") : une Affectation vit PROPOSEE tant qu'aucun
 * chauffeur n'a accepte, puis transite une seule fois vers CONFIRMEE (le
 * premier arrive) ou EXPIREE (perdant de la course sur la meme demande, ou
 * refus explicite). Jamais de retour arriere entre ces etats - une
 * Affectation CONFIRMEE ou EXPIREE est terminale.
 */

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

    // Diffusion-course (trouvaille Mobile) : transporteur auquel cette
    // proposition est diffusee. Denormalise depuis CandidatCoutDto a la
    // creation (source de verite) pour permettre un filtre fiable
    // "mes propositions en attente" - sinon le lien indirect via
    // CapaciteEnAttente.transporteurId est nullable et incomplet. Nullable =
    // capacite sans transporteur identifie, aucune proposition exposee.
    @Column(name = "transporteur_id")
    private UUID transporteurId;

    // EF-GEO-01 ("rattacher toute mission a un axe") - nullable car une
    // demande sans axe connu au moment de sa publication reste possible
    // en mode degrade (cf DemandeAvecCandidats.axeId, deja nullable).
    @Column(name = "axe_id")
    private UUID axeId;

    @Column(name = "cycle_matching_id")
    private UUID cycleMatchingId;

    // EF-MAT-05/07 (Sprint 11, capacite dynamique) - indispensable au
    // sequencement L2, absent avant V12. Nullable : une affectation deja
    // creee avant ce correctif n'a pas cette donnee retroactivement.
    @Column(name = "poids_taxable_kg", precision = 12, scale = 3)
    private BigDecimal poidsTaxableKg;

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

    // Diffusion-course (compte a rebours Mobile) : horodatage au-dela duquel
    // cette proposition PROPOSEE expire par timeout (et non plus seulement
    // "course perdue" / "refus"). Passe par ExpirationPropositionService.
    // Nullable = Affectation creee avant ce mecanisme (V28), expiree par
    // defaut a la prochaine passe de la tache.
    @Column(name = "expire_a")
    private Instant expireA;

    // EF-MAT-08/09, ENF-SEC-03 (idempotence) - null tant que la livraison
    // n'a pas ete executee. Utilise uniquement pour les affectations FTL
    // simples (jamais sequencees en Tournee) : le cas consolide a deja son
    // propre marqueur d'etat via EtapeTournee.Etat.EXECUTEE.
    @Column(name = "livraison_executee_le")
    private Instant livraisonExecuteeLe;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutAffectation statut;

    // Diffusion-course + audit de suivi Mobile : connus au moment L1 (candidat
    // retenu / demande d'origine), persistes ici pour rester disponibles au
    // moment de la confirmation differee (AffectationConfirmationService),
    // qui n'a pas acces au contexte L1 d'origine. Tous nullables : une
    // Affectation creee avant ce correctif n'a pas ces donnees retroactivement.
    @Column(name = "vehicule_id")
    private UUID vehiculeId;

    @Column(name = "type_emballage_nom", length = 150)
    private String typeEmballageNom;

    @Column(name = "quantite")
    private Integer quantite;

    @Column(name = "destinataire_nom", length = 150)
    private String destinataireNom;

    // Diffusion-course (affichage Mobile) : libelles d'origine/destination,
    // connus au moment L1 (comme PropositionEmiseEvent) pour l'ecran
    // propositions du chauffeur.
    @Column(name = "origine_nom", length = 200)
    private String origineNom;

    @Column(name = "destination_nom", length = 200)
    private String destinationNom;

    @Column(name = "destinataire_telephone", length = 30)
    private String destinataireTelephone;

    @Column(name = "mode_collecte", length = 30)
    private String modeCollecte;

    @Column(name = "type_disponibilite", length = 30)
    private String typeDisponibilite;

    @Column(name = "poids_total_kg")
    private Double poidsTotalKg;

    @Column(name = "grande_valeur")
    private Boolean grandeValeur;

    protected Affectation() {
        // Requis par JPA.
    }

    // Constructeur complet : c'est AffectationL1Service qui l'utilise, au
    // moment ou une affectation valide (capaciteId != null) sort du solveur
    // Kuhn-Munkres - jamais construit ailleurs.
    public Affectation(UUID demandeId, UUID capaciteId, UUID transporteurId, UUID cycleMatchingId, UUID axeId,
                        BigDecimal poidsTaxableKg,
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
                        BigDecimal montantVerseTransporteur, boolean tarificationModeDegrade,
                        UUID vehiculeId, String typeEmballageNom, Integer quantite,
                        String destinataireNom, String destinataireTelephone,
                        String modeCollecte, String typeDisponibilite,
                        Double poidsTotalKg, Boolean grandeValeur,
                        String origineNom, String destinationNom, Instant expireA) {
        this.demandeId = demandeId;
        this.capaciteId = capaciteId;
        this.transporteurId = transporteurId;
        this.cycleMatchingId = cycleMatchingId;
        this.axeId = axeId;
        this.poidsTaxableKg = poidsTaxableKg;
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
        this.vehiculeId = vehiculeId;
        this.typeEmballageNom = typeEmballageNom;
        this.quantite = quantite;
        this.destinataireNom = destinataireNom;
        this.destinataireTelephone = destinataireTelephone;
        this.modeCollecte = modeCollecte;
        this.typeDisponibilite = typeDisponibilite;
        this.poidsTotalKg = poidsTotalKg;
        this.grandeValeur = grandeValeur;
        this.origineNom = origineNom;
        this.destinationNom = destinationNom;
        this.expireA = expireA;
        // Diffusion-course : toute Affectation nait PROPOSEE, jamais
        // directement CONFIRMEE - meme la "meilleure" selon Kuhn-Munkres
        // n'est qu'une candidate diffusee parmi d'autres tant qu'aucun
        // chauffeur n'a explicitement accepte (DemandeAccepteeEvent).
        this.statut = StatutAffectation.PROPOSEE;
    }

    @PrePersist
    void onCreate() {
        this.dateCreation = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDemandeId() { return demandeId; }
    public UUID getCapaciteId() { return capaciteId; }
    public UUID getTransporteurId() { return transporteurId; }
    public UUID getCycleMatchingId() { return cycleMatchingId; }
    public UUID getAxeId() { return axeId; }
    public BigDecimal getPoidsTaxableKg() { return poidsTaxableKg; }
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
    public Instant getExpireA() { return expireA; }

    public Instant getLivraisonExecuteeLe() { return livraisonExecuteeLe; }

    /**
     * EF-MAT-08/09 - transition idempotente, meme principe que
     * EtapeTournee.marquerExecutee() : ne renvoie true qu'au moment exact de
     * la premiere execution, jamais sur une redelivrance Kafka du meme
     * evenement (ENF-SEC-03).
     *
     * @return true si CETTE invocation vient de marquer la livraison
     *         (premiere fois) ; false si elle etait deja marquee (evenement
     *         redelivre, aucun effet de bord a declencher a nouveau).
     */
    public boolean marquerLivraisonExecuteeSiNecessaire() {
        if (livraisonExecuteeLe != null) {
            return false;
        }
        this.livraisonExecuteeLe = Instant.now();
        return true;
    }

    public StatutAffectation getStatut() { return statut; }

    public UUID getVehiculeId() { return vehiculeId; }
    public String getTypeEmballageNom() { return typeEmballageNom; }
    public Integer getQuantite() { return quantite; }
    public String getDestinataireNom() { return destinataireNom; }
    public String getOrigineNom() { return origineNom; }
    public String getDestinationNom() { return destinationNom; }
    public String getDestinataireTelephone() { return destinataireTelephone; }
    public String getModeCollecte() { return modeCollecte; }
    public String getTypeDisponibilite() { return typeDisponibilite; }
    public Double getPoidsTotalKg() { return poidsTotalKg; }
    public Boolean getGrandeValeur() { return grandeValeur; }

    /**
     * Transition terminale : ne fait rien si deja CONFIRMEE ou EXPIREE (evite
     * qu'un evenement redelivre - ENF-SEC-03 - ne repasse une Affectation
     * terminale a un etat different). L'atomicite reelle face a la
     * concurrence (deux chauffeurs qui acceptent au meme instant) vient de
     * AffectationRepository.confirmerSiProposee - cette methode ne fait que
     * refleter en memoire un etat deja garanti coherent par la base.
     */
    void marquerExpireeSiProposee() {
        if (statut == StatutAffectation.PROPOSEE) {
            this.statut = StatutAffectation.EXPIREE;
        }
    }
}
