package com.flysoft.fretcorridor.ida.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// EF-IDA-05 : personnes morales, leurs collaborateurs et leurs rôles
@Entity
@Table(name = "organisations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organisation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String raisonSociale;

    private String numeroRegistreCommerce; // optionnel au niveau 1, requis au niveau 2

    @Column(nullable = false)
    private String tenantId;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
