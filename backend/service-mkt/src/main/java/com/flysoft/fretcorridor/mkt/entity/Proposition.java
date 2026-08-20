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

    // RG-039/EF-MKT-08 : le chargeur accepte une des au plus 3 propositions
    // reçues pour une demande -- accepterEnRang() (DemandeService) marque
    // celle-ci ACCEPTEE et les autres EXPIREE. La réservation atomique de
    // capacité (EF-MKT-08, "décrémenter réellement chez le transporteur")
    // reste hors périmètre de ce champ : decrementer() (service-cap) exige
    // que l'appelant soit le TENANT du transporteur propriétaire de la
    // capacité, jamais celui du chargeur qui accepte -- un pont cross-tenant
    // de confiance n'existe pas encore, à construire séparément.
    public enum Statut { EN_ATTENTE, ACCEPTEE, EXPIREE }

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Statut statut = Statut.EN_ATTENTE;
}
