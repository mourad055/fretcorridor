package com.flysoft.fretcorridor.ida.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// RG-011 : le dépôt d'au moins une pièce, combiné à l'identité déclarée,
// conditionne le passage au niveau 1 (EF-IDA-03). La vérification des
// pièces (niveau 2) reste hors périmètre — ce n'est ici qu'un dépôt.
@Entity
@Table(name = "pieces_justificatives")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PieceJustificative {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "acteur_id", nullable = false)
    private Acteur acteur;

    @Column(nullable = false)
    private String typeDocument; // ex: "CNI", "PERMIS" — référentiel par pays/acteur (EF-IDA-04) hors périmètre actuel

    @Column(nullable = false)
    private String objectKey; // clé objet MinIO — jamais d'URL publique stockée

    @Builder.Default
    private LocalDateTime dateDepot = LocalDateTime.now();
}
