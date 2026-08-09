package com.flysoft.fretcorridor.mkt.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Persistance des PropositionEmise recues de service-opt (Moteur), pour que
// GET /api/demandes/{id}/propositions ait une vraie source au lieu du stub
// vide (S5). Un eventId unique = idempotence si Kafka rejoue le message.
@Entity
@Table(name = "propositions", uniqueConstraints = @UniqueConstraint(columnNames = "eventId"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proposition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false)
    private UUID demandeId;

    private UUID cycleMatchingId;
    private UUID capaciteId;
    private UUID missionId;
    private UUID axeId;

    @Column(nullable = false)
    private Integer rang;

    private String motifClassement;

    private BigDecimal prixTransport;
    private BigDecimal commissionPlateforme;

    @Builder.Default
    private String devise = "XAF";

    private Double distanceEstimeeMetres;
    private Integer dureeEstimeeSecondes;
    private String origineNom;
    private String destinationNom;

    @Column(nullable = false)
    private LocalDateTime horodatageEmission;

    @Builder.Default
    private LocalDateTime dateReception = LocalDateTime.now();
}
