package com.flysoft.fretcorridor.mkt.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// UC-MKT-01 : publication d'une demande de transport (flux nominal uniquement
// pour cette passe — les flux alternatifs A1-A5 et E1-E5 du CDC §8 sont
// volontairement reportés, voir README du patch).
@Entity
@Table(name = "demandes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Demande {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID clientActeurId; // référence à Acteur (service-ida) — pas de FK inter-service

    // Où
    @Column(nullable = false)
    private String villeDepart;

    @Column(nullable = false)
    private String villeArrivee;

    // Quoi / Combien
    @ManyToOne
    @JoinColumn(name = "type_emballage_id", nullable = false)
    private CatalogueEmballage typeEmballage;

    @Column(nullable = false)
    private Integer quantite;

    // Calculés automatiquement à partir du catalogue (EF-MKT-04)
    @Column(nullable = false)
    private Double poidsTotalKg;

    @Column(nullable = false)
    private Double volumeTotalM3;

    @Column(nullable = false)
    private Double poidsTaxableKg;

    // Nature particulière (quatre commutateurs, RG du flux nominal)
    @Builder.Default private Boolean fragile = false;
    @Builder.Default private Boolean perissable = false;
    @Builder.Default private Boolean dangereuse = false;
    @Builder.Default private Boolean grandeValeur = false;

    // Quand
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDisponibilite typeDisponibilite;

    private LocalDateTime dateDisponibilite; // si DATE_PRECISE ou PLAGE (début)

    // Comment
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModeCollecte modeCollecte;

    // Qui reçoit
    @Column(nullable = false)
    private String destinataireNom;

    @Column(nullable = false)
    private String destinataireTelephone;

    // RG-036 : niveau de confiance dimensionnelle — CATALOGUE uniquement pour
    // l'instant (flux "Autre" avec photos = A1, reporté)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NiveauConfiance niveauConfianceDimensionnelle = NiveauConfiance.DEDUIT_DU_CATALOGUE;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutDemande statut = StatutDemande.PUBLIEE;

    @Column(nullable = false)
    private String tenantId;

    // Champs requis par le cycle de matching (Moteur, S5) - ajoutes pour la
    // publication de DemandePubliee. Nullable au niveau entite : une demande
    // dont l'axe n'a pas encore ete resolu (ex. villes non couvertes) doit
    // pouvoir exister en base sans bloquer la creation, meme si publier()
    // exige ces valeurs avant d'emettre l'evenement (cf DemandeService).
    private UUID axeId;
    private Double origineLatitude;
    private Double origineLongitude;
    private Double destinationLatitude;
    private Double destinationLongitude;

    // JSON libre (pas de colonnes dediees par critere) : coherent avec
    // l'anti-patron "jamais de bareme en dur" deja applique cote MAT/OPT -
    // un nouveau critere de matching ne doit jamais exiger de migration ici.
    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private java.util.Map<String, Double> valeursCriteres;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    public enum TypeDisponibilite { DES_QUE_POSSIBLE, DATE_PRECISE, PLAGE }
    public enum ModeCollecte { DOMICILE, POINT_RELAIS }
    public enum NiveauConfiance { DECLARE_PRECIS, DEDUIT_DU_CATALOGUE, A_CONFIRMER }
    public enum StatutDemande { PUBLIEE, AXE_NON_DESSERVI, ANNULEE }
}
