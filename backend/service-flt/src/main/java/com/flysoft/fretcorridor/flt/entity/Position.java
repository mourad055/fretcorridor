package com.flysoft.fretcorridor.flt.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// EF-TRK-01/04 : position horodatée, tolérante à la connectivité intermittente.
// missionId référence service-exe — pas de FK inter-service (architecture microservices).
@Entity
@Table(name = "positions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID missionId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private LocalDateTime horodatage; // capturé côté chauffeur, distinct de la réception serveur

    @Column(nullable = false)
    private String tenantId;

    @Builder.Default
    private LocalDateTime dateReception = LocalDateTime.now();
}
