package com.fretcorridor.trk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Position GPS ingeree depuis l'evenement Kafka PositionBrute (module FLT,
 * Mobile). Une ligne = une capture, jamais mise a jour ensuite (immuable,
 * meme logique que les autres entites du perimetre Moteur).
 *
 * eventId porte la contrainte UNIQUE en base (cf migration V2) : c'est elle,
 * pas du code applicatif, qui garantit l'idempotence face aux re-envois
 * (ENF-SEC-03) - un doublon leve une DataIntegrityViolationException que
 * PositionBruteListener attrape et ignore proprement.
 */
@Entity
@Table(name = "position", schema = "trk")
public class Position {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "vehicule_id", nullable = false)
    private UUID vehiculeId;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "source_capture", nullable = false, length = 30)
    private String sourceCapture;

    // Nullable : l'appareil ne rapporte pas toujours une precision GPS.
    @Column(name = "precision_metres")
    private Double precisionMetres;

    @Column(name = "horodatage_capture", nullable = false)
    private Instant horodatageCapture;

    @Column(name = "horodatage_transmission", nullable = false)
    private Instant horodatageTransmission;

    // Horodatage de reception cote TRK - distinct des deux precedents,
    // troisieme point de reference utile pour diagnostiquer un ecart de
    // connectivite (transmission tardive) vs un ecart de traitement (ingestion
    // tardive apres reception).
    @Column(name = "horodatage_ingestion", nullable = false, updatable = false)
    private Instant horodatageIngestion;

    protected Position() {
        // requis par JPA
    }

    public Position(UUID eventId, UUID missionId, UUID vehiculeId, double latitude, double longitude,
                     String sourceCapture, Double precisionMetres,
                     Instant horodatageCapture, Instant horodatageTransmission) {
        this.eventId = eventId;
        this.missionId = missionId;
        this.vehiculeId = vehiculeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sourceCapture = sourceCapture;
        this.precisionMetres = precisionMetres;
        this.horodatageCapture = horodatageCapture;
        this.horodatageTransmission = horodatageTransmission;
    }

    @PrePersist
    void onCreate() {
        this.horodatageIngestion = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getMissionId() { return missionId; }
    public UUID getVehiculeId() { return vehiculeId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getSourceCapture() { return sourceCapture; }
    public Double getPrecisionMetres() { return precisionMetres; }
    public Instant getHorodatageCapture() { return horodatageCapture; }
    public Instant getHorodatageTransmission() { return horodatageTransmission; }
    public Instant getHorodatageIngestion() { return horodatageIngestion; }
}
