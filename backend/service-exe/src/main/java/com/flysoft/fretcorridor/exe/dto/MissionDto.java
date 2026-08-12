package com.flysoft.fretcorridor.exe.dto;

import com.flysoft.fretcorridor.exe.entity.EtapeMission;
import com.flysoft.fretcorridor.exe.entity.Mission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class MissionDto {

    // S7 : vue résumée pour la liste "mes missions" côté chauffeur
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MissionResumeResponse {
        private UUID missionId;
        private String statut;
        private String origineNom;
        private String destinationNom;
        private LocalDateTime dateCreation;

        public static MissionResumeResponse fromEntity(Mission m) {
            return MissionResumeResponse.builder()
                    .missionId(m.getId())
                    .statut(m.getStatut().name())
                    .origineNom(m.getOrigineNom())
                    .destinationNom(m.getDestinationNom())
                    .dateCreation(m.getDateCreation())
                    .build();
        }
    }

    // S7 : le chauffeur ajoute une étape (prise en charge/en transit/livraison/incident)
    @Data
    public static class AjouterEtapeRequest {
        @NotNull private EtapeMission.TypeEtape type;
        @NotBlank private String libelle;
        private LocalDateTime horodatageCapture;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EtapeResponse {
        private String type;
        private String libelle;
        private LocalDateTime horodatageCapture;
        private LocalDateTime horodatageTransmission;

        public static EtapeResponse fromEntity(EtapeMission e) {
            return EtapeResponse.builder()
                    .type(e.getType().name())
                    .libelle(e.getLibelle())
                    .horodatageCapture(e.getHorodatageCapture())
                    .horodatageTransmission(e.getHorodatageTransmission())
                    .build();
        }
    }

    // UC-EXE-01 côté Client : chronologie lecture seule
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChronologieResponse {
        private UUID missionId;
        private String statut;
        private List<EtapeResponse> etapes;

        public static ChronologieResponse fromEntity(Mission m, List<EtapeMission> etapes) {
            return ChronologieResponse.builder()
                    .missionId(m.getId())
                    .statut(m.getStatut().name())
                    .etapes(etapes.stream().map(EtapeResponse::fromEntity).toList())
                    .build();
        }
    }
}
