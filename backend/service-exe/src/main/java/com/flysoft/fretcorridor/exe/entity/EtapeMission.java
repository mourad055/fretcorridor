package com.flysoft.fretcorridor.exe.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// EF-EXE-02/04 : étapes d'une mission, avec horodatage de capture distinct
// de l'horodatage de transmission.
@Entity
@Table(name = "etapes_mission")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtapeMission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeEtape type;

    @Column(nullable = false)
    private String libelle; // ex: "Prise en charge à Yaoundé"

    private LocalDateTime horodatageCapture; // EF-EXE-04 : au moment réel de l'action

    @Builder.Default
    private LocalDateTime horodatageTransmission = LocalDateTime.now();

    public enum TypeEtape { PRISE_EN_CHARGE, EN_TRANSIT, LIVRAISON, INCIDENT }
}
