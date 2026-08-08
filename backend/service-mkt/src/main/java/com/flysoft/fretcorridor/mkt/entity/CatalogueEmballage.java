package com.flysoft.fretcorridor.mkt.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

// EF-MKT-03 / RG-035 : référentiel des types de marchandises, versionné.
// Les valeurs de poids/volume par défaut sont des estimations MVP — à affiner
// avec une vraie observation de terrain du marché camerounais (RG-035).
@Entity
@Table(name = "catalogue_emballages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogueEmballage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nom; // "Sac de ciment", "Carton", "Palette"...

    @Column(nullable = false)
    private String icone; // code d'icône Material côté mobile (ex: "inventory_2")

    @Column(nullable = false)
    private Double poidsUnitaireKg;

    @Column(nullable = false)
    private Double volumeUnitaireM3;

    @Builder.Default
    private Boolean fragileParDefaut = false;

    @Builder.Default
    private Boolean gerbable = true;

    @Builder.Default
    private Boolean actif = true;

    private Integer ordreAffichage;
}
