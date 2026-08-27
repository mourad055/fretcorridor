package com.fretcorridor.opt.domain;

import com.fretcorridor.dto.PointGeoDto;
import com.fretcorridor.opt.client.ProfilCamionDto;
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

/**
 * Capacite en attente d'un cycle de matching (EF-MAT-01, "par cycles a
 * fenetre"). Voir MatchingCycleService pour la consommation du lot.
 *
 * transporteurId/vehiculeId : nullable, cf CapaciteDeclareeEvent - fix du bug
 * S7 remonte par Personne 1 (Mobile), en attente de la publication reelle
 * cote service-cap.
 */
@Entity
@Table(name = "capacite_en_attente", schema = "opt")
public class CapaciteEnAttente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "capacite_id", nullable = false)
    private UUID capaciteId;

    @Column(name = "axe_id", nullable = false)
    private UUID axeId;

    @Column(name = "transporteur_id")
    private UUID transporteurId;

    @Column(name = "vehicule_id")
    private UUID vehiculeId;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valeurs_criteres", nullable = false, columnDefinition = "jsonb")
    private Map<String, Double> valeursCriteres;

    @Column(name = "position_latitude")
    private Double positionLatitude;

    @Column(name = "position_longitude")
    private Double positionLongitude;

    @Column(name = "profil_hauteur_m")
    private Double profilHauteurM;

    @Column(name = "profil_largeur_m")
    private Double profilLargeurM;

    @Column(name = "profil_longueur_m")
    private Double profilLongueurM;

    @Column(name = "profil_poids_max_t")
    private Double profilPoidsMaxT;

    @Column(name = "profil_charge_essieu_max_t")
    private Double profilChargeEssieuMaxT;

    @Column(name = "profil_nb_essieux")
    private Integer profilNbEssieux;

    @Column(name = "profil_matieres_dangereuses")
    private Boolean profilMatieresDangereuses;

    @Column(name = "type_vehicule")
    private String typeVehicule;

    // EF-CAP-07 / CDC S8.6.1 point 3 (capacite dynamique) - grandeur
    // reellement disponible, distincte de profilPoidsMaxT (plafond
    // vehicule). Non nullable en base : coherent avec le caractere
    // requis du champ dans capacite-declaree.yaml.
    @Column(name = "capacite_residuelle_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal capaciteResiduelleKg;

    @Column(name = "volume_residuel_m3", precision = 12, scale = 3)
    private BigDecimal volumeResiduelM3;

    @Column(nullable = false)
    private boolean traitee = false;

    @Column(name = "date_reception", nullable = false, updatable = false)
    private Instant dateReception;

    protected CapaciteEnAttente() {
        // requis par JPA
    }

    public CapaciteEnAttente(UUID capaciteId, UUID axeId, UUID transporteurId, UUID vehiculeId,
                              UUID eventId, Map<String, Double> valeursCriteres,
                              PointGeoDto position, ProfilCamionDto profilCamion, String typeVehicule,
                              BigDecimal capaciteResiduelleKg, BigDecimal volumeResiduelM3) {
        this.capaciteId = capaciteId;
        this.axeId = axeId;
        this.transporteurId = transporteurId;
        this.vehiculeId = vehiculeId;
        this.eventId = eventId;
        this.valeursCriteres = valeursCriteres;
        if (position != null) {
            this.positionLatitude = position.latitude();
            this.positionLongitude = position.longitude();
        }
        if (profilCamion != null) {
            this.profilHauteurM = profilCamion.hauteurMetres();
            this.profilLargeurM = profilCamion.largeurMetres();
            this.profilLongueurM = profilCamion.longueurMetres();
            this.profilPoidsMaxT = profilCamion.poidsMaxTonnes();
            this.profilChargeEssieuMaxT = profilCamion.chargeMaxParEssieuTonnes();
            this.profilNbEssieux = profilCamion.nombreEssieux();
            this.profilMatieresDangereuses = profilCamion.matieresDangereuses();
        }
        this.typeVehicule = typeVehicule;
        this.capaciteResiduelleKg = capaciteResiduelleKg;
        this.volumeResiduelM3 = volumeResiduelM3;
    }

    @PrePersist
    void onCreate() {
        this.dateReception = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCapaciteId() { return capaciteId; }
    public UUID getAxeId() { return axeId; }
    public UUID getTransporteurId() { return transporteurId; }
    public UUID getVehiculeId() { return vehiculeId; }
    public Map<String, Double> getValeursCriteres() { return valeursCriteres; }
    public boolean isTraitee() { return traitee; }
    public void marquerTraitee() { this.traitee = true; }

    // UC-MAT-02 (CDC) : voir le commentaire equivalent sur
    // DemandeEnAttente.remettreEnAttente() -- meme raisonnement.
    public void remettreEnAttente() { this.traitee = false; }

    public PointGeoDto getPosition() {
        return (positionLatitude == null || positionLongitude == null)
                ? null : new PointGeoDto(positionLatitude, positionLongitude);
    }

    public ProfilCamionDto getProfilCamion() {
        if (profilHauteurM == null && profilLargeurM == null && profilLongueurM == null
                && profilPoidsMaxT == null && profilChargeEssieuMaxT == null && profilNbEssieux == null) {
            return null;
        }
        return new ProfilCamionDto(profilHauteurM, profilLargeurM, profilLongueurM,
                profilPoidsMaxT, profilChargeEssieuMaxT, profilNbEssieux,
                Boolean.TRUE.equals(profilMatieresDangereuses));
    }

    public String getTypeVehicule() { return typeVehicule; }
    public BigDecimal getCapaciteResiduelleKg() { return capaciteResiduelleKg; }
    public BigDecimal getVolumeResiduelM3() { return volumeResiduelM3; }
    // RG-105 : age de la capacite dans la file - conditionne son eligibilite
    // a la fenetre de traitement de l'axe (fenetre adaptative par axe).
    public Instant getDateReception() { return dateReception; }
}
