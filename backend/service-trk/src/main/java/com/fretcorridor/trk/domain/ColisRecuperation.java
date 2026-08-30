package com.fretcorridor.trk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Etat de recuperation du colis d'une mission, cote TRK (point 6 du plan de
 * reorientation : "colis recupere = position chauffeur").
 *
 * Avant cet enregistrement, le suivi affiche la POSITION ESTIMEE du colis
 * (son point d'enlevement, porte par l'affectation cote OPT) ; a partir de
 * l'instant ou l'enlevement est execute (EtapeExecuteeEvent typeEtape =
 * ENLEVEMENT), le colis est a bord du vehicule et le suivi doit basculer sur
 * la position GPS temps reel du chauffeur.
 *
 * Une seule ligne par mission (PK = mission_id) : la recuperation est un
 * evenement unique et idempotent. Le listener d'enlevement gere explicitement
 * le doublon Kafka (jamais de seconde ligne ni d'ecrasement).
 */
@Entity
@Table(name = "colis_recuperation", schema = "trk")
public class ColisRecuperation {

    @Id
    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "horodatage_enlevement", nullable = false)
    private Instant horodatageEnlevement;

    @Column(name = "horodatage_ingestion", nullable = false, updatable = false)
    private Instant horodatageIngestion;

    protected ColisRecuperation() {
        // requis par JPA
    }

    public ColisRecuperation(UUID missionId, Instant horodatageEnlevement) {
        this.missionId = missionId;
        this.horodatageEnlevement = horodatageEnlevement;
    }

    @PrePersist
    void onCreate() {
        this.horodatageIngestion = Instant.now();
    }

    public UUID getMissionId() {
        return missionId;
    }

    public Instant getHorodatageEnlevement() {
        return horodatageEnlevement;
    }

    public Instant getHorodatageIngestion() {
        return horodatageIngestion;
    }
}
