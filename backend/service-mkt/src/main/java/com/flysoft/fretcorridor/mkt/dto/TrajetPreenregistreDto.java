package com.flysoft.fretcorridor.mkt.dto;

import com.flysoft.fretcorridor.mkt.entity.TrajetPreenregistre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

public class TrajetPreenregistreDto {

    @Data
    public static class EnregistrerRequest {
        @NotBlank private String nom;
        @NotBlank private String villeDepart;
        @NotBlank private String villeArrivee;
        @NotNull private UUID typeEmballageId;
        @NotNull @Positive private Integer quantite;

        private Boolean fragile = false;
        private Boolean perissable = false;
        private Boolean dangereuse = false;
        private Boolean grandeValeur = false;

        @NotBlank private String categorieMarchandise;
        @NotBlank private String modeCollecte;

        @NotBlank private String destinataireNom;
        @NotBlank private String destinataireTelephone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrajetResponse {
        private UUID id;
        private String nom;
        private String villeDepart;
        private String villeArrivee;
        private UUID typeEmballageId;
        private String typeEmballageNom;
        private Integer quantite;
        private Boolean fragile;
        private Boolean perissable;
        private Boolean dangereuse;
        private Boolean grandeValeur;
        private String categorieMarchandise;
        private String modeCollecte;
        private String destinataireNom;
        private String destinataireTelephone;
        private LocalDateTime dateCreation;

        public static TrajetResponse fromEntity(TrajetPreenregistre t) {
            return TrajetResponse.builder()
                    .id(t.getId())
                    .nom(t.getNom())
                    .villeDepart(t.getVilleDepart())
                    .villeArrivee(t.getVilleArrivee())
                    .typeEmballageId(t.getTypeEmballage().getId())
                    .typeEmballageNom(t.getTypeEmballage().getNom())
                    .quantite(t.getQuantite())
                    .fragile(t.getFragile())
                    .perissable(t.getPerissable())
                    .dangereuse(t.getDangereuse())
                    .grandeValeur(t.getGrandeValeur())
                    .categorieMarchandise(t.getCategorieMarchandise() != null ? t.getCategorieMarchandise().name() : null)
                    .modeCollecte(t.getModeCollecte().name())
                    .destinataireNom(t.getDestinataireNom())
                    .destinataireTelephone(t.getDestinataireTelephone())
                    .dateCreation(t.getDateCreation())
                    .build();
        }
    }
}
