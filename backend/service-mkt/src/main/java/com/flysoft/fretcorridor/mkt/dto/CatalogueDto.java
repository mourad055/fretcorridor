package com.flysoft.fretcorridor.mkt.dto;

import com.flysoft.fretcorridor.mkt.entity.CatalogueEmballage;
import lombok.*;
import java.util.UUID;

public class CatalogueDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmballageResponse {
        private UUID id;
        private String nom;
        private String icone;
        private Double poidsUnitaireKg;
        private Double volumeUnitaireM3;
        private Boolean fragileParDefaut;

        public static EmballageResponse fromEntity(CatalogueEmballage e) {
            return EmballageResponse.builder()
                    .id(e.getId())
                    .nom(e.getNom())
                    .icone(e.getIcone())
                    .poidsUnitaireKg(e.getPoidsUnitaireKg())
                    .volumeUnitaireM3(e.getVolumeUnitaireM3())
                    .fragileParDefaut(e.getFragileParDefaut())
                    .build();
        }
    }
}
