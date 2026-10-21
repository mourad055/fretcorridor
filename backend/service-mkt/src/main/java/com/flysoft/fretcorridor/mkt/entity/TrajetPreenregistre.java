package com.flysoft.fretcorridor.mkt.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// Trajet preenregistre nomme (plan-fretcorridor-reorientation.md §5,
// "trajets preenregistres") : raccourci de saisie uniquement, pre-remplit
// les memes champs qu'une publication classique -- jamais publie tout
// seul, une Demande reelle est toujours creee via DemandeService.publier()
// a partir de ces valeurs. N'inclut PAS typeDisponibilite/dateDisponibilite :
// la disponibilite se redecide a chaque publication, ce n'est pas un
// attribut stable du trajet (contrairement a l'axe, la marchandise et le
// destinataire, qui le sont pour un trajet recurrent).
@Entity
@Table(name = "trajets_preenregistres")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrajetPreenregistre {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID clientActeurId; // reference a Acteur (service-ida) — pas de FK inter-service

    @Column(nullable = false)
    private String tenantId;

    // Nom choisi par le client (ex. "Livraison hebdo bureau Yaoundé").
    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String villeDepart;

    @Column(nullable = false)
    private String villeArrivee;

    @ManyToOne
    @JoinColumn(name = "type_emballage_id", nullable = false)
    private CatalogueEmballage typeEmballage;

    @Column(nullable = false)
    private Integer quantite;

    @Enumerated(EnumType.STRING)
    private Demande.CategorieMarchandise categorieMarchandise;

    @Builder.Default private Boolean fragile = false;
    @Builder.Default private Boolean perissable = false;
    @Builder.Default private Boolean dangereuse = false;
    @Builder.Default private Boolean grandeValeur = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Demande.ModeCollecte modeCollecte;

    @Column(nullable = false)
    private String destinataireNom;

    @Column(nullable = false)
    private String destinataireTelephone;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
