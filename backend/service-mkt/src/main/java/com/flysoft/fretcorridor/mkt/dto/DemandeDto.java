package com.flysoft.fretcorridor.mkt.dto;

import com.flysoft.fretcorridor.mkt.entity.Demande;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class DemandeDto {

    @Data
    public static class PublierRequest {
        @NotBlank private String villeDepart;
        @NotBlank private String villeArrivee;
        @NotNull private UUID typeEmballageId;
        @NotNull @Positive private Integer quantite;

        private Boolean fragile = false;
        private Boolean perissable = false;
        private Boolean dangereuse = false;
        private Boolean grandeValeur = false;

        @NotBlank private String typeDisponibilite; // DES_QUE_POSSIBLE / DATE_PRECISE / PLAGE
        private LocalDateTime dateDisponibilite;

        @NotBlank private String modeCollecte; // DOMICILE / POINT_RELAIS

        @NotBlank private String destinataireNom;
        @NotBlank private String destinataireTelephone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DemandeResponse {
        private UUID id;
        private String villeDepart;
        private String villeArrivee;
        private String typeEmballageNom;
        private Integer quantite;
        private Double poidsTotalKg;
        private Double volumeTotalM3;
        private Double poidsTaxableKg;
        private Boolean fragile;
        private Boolean perissable;
        private Boolean dangereuse;
        private Boolean grandeValeur;
        private String typeDisponibilite;
        private String modeCollecte;
        private String destinataireNom;
        private String destinataireTelephone;
        private String statut;
        private LocalDateTime dateCreation;

        public static DemandeResponse fromEntity(Demande d) {
            return DemandeResponse.builder()
                    .id(d.getId())
                    .villeDepart(d.getVilleDepart())
                    .villeArrivee(d.getVilleArrivee())
                    .typeEmballageNom(d.getTypeEmballage().getNom())
                    .quantite(d.getQuantite())
                    .poidsTotalKg(d.getPoidsTotalKg())
                    .volumeTotalM3(d.getVolumeTotalM3())
                    .poidsTaxableKg(d.getPoidsTaxableKg())
                    .fragile(d.getFragile())
                    .perissable(d.getPerissable())
                    .dangereuse(d.getDangereuse())
                    .grandeValeur(d.getGrandeValeur())
                    .typeDisponibilite(d.getTypeDisponibilite().name())
                    .modeCollecte(d.getModeCollecte().name())
                    .destinataireNom(d.getDestinataireNom())
                    .destinataireTelephone(d.getDestinataireTelephone())
                    .statut(d.getStatut().name())
                    .dateCreation(d.getDateCreation())
                    .build();
        }
    }

    // S5 — alimenté par PropositionEmiseListener (service-opt, Moteur)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PropositionResponse {
        private UUID id;
        private Integer rang;
        private String motifClassement;
        private String prixEstime;
        private String statut;
    }
}
