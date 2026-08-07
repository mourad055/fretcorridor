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

    @Column(nullable = false)
    private boolean traitee = false;

    @Column(name = "date_reception", nullable = false, updatable = false)
    private Instant dateReception;

    protected DemandeEnAttente() {
        // requis par JPA
    }

    public DemandeEnAttente(UUID demandeId, UUID axeId, UUID eventId, Map<String, Double> valeursCriteres,
                             PointGeoDto origine, PointGeoDto destination, BigDecimal poidsTaxableKg) {
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
    public BigDecimal getPoidsTaxableKg() { return poidsTaxableKg; }

    public PointGeoDto getOrigine() {
        return (origineLatitude == null || origineLongitude == null)
                ? null : new PointGeoDto(origineLatitude, origineLongitude);
    }

    public PointGeoDto getDestination() {
        return (destinationLatitude == null || destinationLongitude == null)
                ? null : new PointGeoDto(destinationLatitude, destinationLongitude);
    }
}
