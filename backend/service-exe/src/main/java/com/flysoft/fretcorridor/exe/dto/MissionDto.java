package com.flysoft.fretcorridor.exe.dto;

import com.flysoft.fretcorridor.exe.entity.EtapeMission;
import com.flysoft.fretcorridor.exe.entity.EtapeTournee;
import com.flysoft.fretcorridor.exe.entity.Mission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
        private UUID tourneeId;

        public static MissionResumeResponse fromEntity(Mission m) {
            return MissionResumeResponse.builder()
                    .missionId(m.getId())
                    .statut(m.getStatut().name())
                    .origineNom(m.getOrigineNom())
                    .destinationNom(m.getDestinationNom())
                    .dateCreation(m.getDateCreation())
                    .tourneeId(m.getTourneeId())
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
        private UUID tourneeId;
        private UUID vehiculeId;

        public static ChronologieResponse fromEntity(Mission m, List<EtapeMission> etapes) {
            return ChronologieResponse.builder()
                    .missionId(m.getId())
                    .statut(m.getStatut().name())
                    .etapes(etapes.stream().map(EtapeResponse::fromEntity).toList())
                    .tourneeId(m.getTourneeId())
                    .vehiculeId(m.getVehiculeId())
                    .build();
        }
    }

    // S11 : ordre planifié des étapes d'une Tournée consolidée (LTL), avec le
    // statut d'exécution réel de la Mission à laquelle chaque étape se
    // rattache — permet à l'app Chauffeur de dériver l'étape courante sans
    // dupliquer la logique de progression déjà portée par Mission.statut.
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EtapeTourneeResponse {
        private UUID missionId;
        private int rang;
        private String typeEtape;
        private UUID demandeId;
        private double pointLatitude;
        private double pointLongitude;
        private LocalDateTime fenetreDebut;
        private LocalDateTime fenetreFin;
        private String missionStatut;

        public static EtapeTourneeResponse fromEntity(EtapeTournee e, String missionStatut) {
            return EtapeTourneeResponse.builder()
                    .missionId(e.getMissionId())
                    .rang(e.getRang())
                    .typeEtape(e.getTypeEtape().name())
                    .demandeId(e.getDemandeId())
                    .pointLatitude(e.getPointLatitude())
                    .pointLongitude(e.getPointLongitude())
                    .fenetreDebut(e.getFenetreDebut())
                    .fenetreFin(e.getFenetreFin())
                    .missionStatut(missionStatut)
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TourneeResponse {
        private UUID tourneeId;
        private List<EtapeTourneeResponse> etapes;

        public static TourneeResponse of(UUID tourneeId, List<EtapeTournee> etapes, Map<UUID, String> statutParMission) {
            return TourneeResponse.builder()
                    .tourneeId(tourneeId)
                    .etapes(etapes.stream()
                            .map(e -> EtapeTourneeResponse.fromEntity(e, statutParMission.get(e.getMissionId())))
                            .toList())
                    .build();
        }
    }
}
