package com.flysoft.fretcorridor.flt.dto;

import com.flysoft.fretcorridor.flt.entity.Vehicule;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class VehiculeDto {

    @Data
    public static class DeclarerRequest {
        @NotBlank private String typeVehicule;
        private String immatriculation;
        private Double profilHauteurMetres;
        private Double profilLargeurMetres;
        private Double profilLongueurMetres;
        private Double profilPoidsMaxTonnes;
        private Double profilChargeMaxParEssieuTonnes;
        private Integer profilNombreEssieux;
        private boolean profilMatieresDangereuses;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehiculeResponse {
        private UUID id;
        private String typeVehicule;
        private String immatriculation;
        private Double profilHauteurMetres;
        private Double profilLargeurMetres;
        private Double profilLongueurMetres;
        private Double profilPoidsMaxTonnes;
        private Double profilChargeMaxParEssieuTonnes;
        private Integer profilNombreEssieux;
        private boolean profilMatieresDangereuses;
        private LocalDateTime dateCreation;

        public static VehiculeResponse fromEntity(Vehicule v) {
            return VehiculeResponse.builder()
                    .id(v.getId())
                    .typeVehicule(v.getTypeVehicule())
                    .immatriculation(v.getImmatriculation())
                    .profilHauteurMetres(v.getProfilHauteurMetres())
                    .profilLargeurMetres(v.getProfilLargeurMetres())
                    .profilLongueurMetres(v.getProfilLongueurMetres())
                    .profilPoidsMaxTonnes(v.getProfilPoidsMaxTonnes())
                    .profilChargeMaxParEssieuTonnes(v.getProfilChargeMaxParEssieuTonnes())
                    .profilNombreEssieux(v.getProfilNombreEssieux())
                    .profilMatieresDangereuses(v.isProfilMatieresDangereuses())
                    .dateCreation(v.getDateCreation())
                    .build();
        }
    }
}
