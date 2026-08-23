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
        private String typeEmballageNom;
        private Integer quantite;
        private java.math.BigDecimal poidsTaxableKg;
        private String destinataireNom;
        private String destinataireTelephone;
        private String demandeModeCollecte;
        private String typeDisponibilite;
        private Double poidsTotalKg;
        private Boolean grandeValeur;

        public static MissionResumeResponse fromEntity(Mission m) {
            return MissionResumeResponse.builder()
                    .missionId(m.getId())
                    .statut(m.getStatut().name())
                    .origineNom(m.getOrigineNom())
                    .destinationNom(m.getDestinationNom())
                    .dateCreation(m.getDateCreation())
                    .tourneeId(m.getTourneeId())
                    .typeEmballageNom(m.getTypeEmballageNom())
                    .quantite(m.getQuantite())
                    .poidsTaxableKg(m.getPoidsTaxableKg())
                    .destinataireNom(m.getDestinataireNom())
                    .destinataireTelephone(m.getDestinataireTelephone())
                    .demandeModeCollecte(m.getDemandeModeCollecte())
                    .typeDisponibilite(m.getTypeDisponibilite())
                    .poidsTotalKg(m.getPoidsTotalKg())
                    .grandeValeur(m.getGrandeValeur())
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
        private String origineNom;
        private String destinationNom;
        private String typeEmballageNom;
        private Integer quantite;
        private java.math.BigDecimal poidsTaxableKg;
        private String destinataireNom;
        private String destinataireTelephone;

        public static ChronologieResponse fromEntity(Mission m, List<EtapeMission> etapes) {
            return ChronologieResponse.builder()
                    .missionId(m.getId())
                    .statut(m.getStatut().name())
                    .etapes(etapes.stream().map(EtapeResponse::fromEntity).toList())
                    .tourneeId(m.getTourneeId())
                    .vehiculeId(m.getVehiculeId())
                    .origineNom(m.getOrigineNom())
                    .destinationNom(m.getDestinationNom())
                    .typeEmballageNom(m.getTypeEmballageNom())
                    .quantite(m.getQuantite())
                    .poidsTaxableKg(m.getPoidsTaxableKg())
                    .destinataireNom(m.getDestinataireNom())
                    .destinataireTelephone(m.getDestinataireTelephone())
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
        // S16/EF-MAT-13 (audit de suivi, 23 août) : charge par essieu à cet
        // état de la tournée (approximation uniforme, cf javadoc
        // service-opt/OracleChargementService) - null tant que
        // PlanChargementConfirme n'a pas été ingéré pour ce rang (tournée
        // pas encore confirmée par l'oracle, ou événement pas encore reçu),
        // jamais une valeur inventée en remplacement.
        private java.util.Map<String, Object> chargesParEssieu;

        public static EtapeTourneeResponse fromEntity(EtapeTournee e, String missionStatut,
                                                        java.util.Map<String, Object> chargesParEssieu) {
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
                    .chargesParEssieu(chargesParEssieu)
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

        public static TourneeResponse of(UUID tourneeId, List<EtapeTournee> etapes, Map<UUID, String> statutParMission,
                                          Map<Integer, java.util.Map<String, Object>> chargesParEssieuParRang) {
            return TourneeResponse.builder()
                    .tourneeId(tourneeId)
                    .etapes(etapes.stream()
                            .map(e -> EtapeTourneeResponse.fromEntity(e, statutParMission.get(e.getMissionId()),
                                    chargesParEssieuParRang.get(e.getRang())))
                            .toList())
                    .build();
        }
    }
}
