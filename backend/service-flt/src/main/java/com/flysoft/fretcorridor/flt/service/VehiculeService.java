package com.flysoft.fretcorridor.flt.service;

import com.flysoft.fretcorridor.flt.dto.VehiculeDto;
import com.flysoft.fretcorridor.flt.entity.Vehicule;
import com.flysoft.fretcorridor.flt.repository.VehiculeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// S10 : console de flotte simplifiée (mode transporteur étendu).
@Service
@RequiredArgsConstructor
public class VehiculeService {

    private final VehiculeRepository vehiculeRepository;

    @Transactional
    public VehiculeDto.VehiculeResponse declarer(UUID proprietaireActeurId, String tenantId, VehiculeDto.DeclarerRequest request) {
        Vehicule vehicule = Vehicule.builder()
                .proprietaireActeurId(proprietaireActeurId)
                .typeVehicule(request.getTypeVehicule())
                .immatriculation(request.getImmatriculation())
                .profilHauteurMetres(request.getProfilHauteurMetres())
                .profilLargeurMetres(request.getProfilLargeurMetres())
                .profilLongueurMetres(request.getProfilLongueurMetres())
                .profilPoidsMaxTonnes(request.getProfilPoidsMaxTonnes())
                .profilChargeMaxParEssieuTonnes(request.getProfilChargeMaxParEssieuTonnes())
                .profilNombreEssieux(request.getProfilNombreEssieux())
                .profilMatieresDangereuses(request.isProfilMatieresDangereuses())
                .tenantId(tenantId)
                .build();
        // RG-088 (audit CDC du 19 août) : la contrainte unique (immatriculation,
        // migration Vehicule.java) protège désormais réellement contre le
        // doublon inter-tenant — traduit ici en erreur métier explicite plutôt
        // que de laisser remonter un 500 générique.
        try {
            vehicule = vehiculeRepository.save(vehicule);
        } catch (DataIntegrityViolationException doublon) {
            throw new ImmatriculationDejaUtiliseeException(request.getImmatriculation());
        }
        return VehiculeDto.VehiculeResponse.fromEntity(vehicule);
    }

    @Transactional(readOnly = true)
    public List<VehiculeDto.VehiculeResponse> listerMesVehicules(UUID proprietaireActeurId, String tenantId) {
        return vehiculeRepository.findByProprietaireActeurIdAndTenantIdOrderByDateCreationDesc(proprietaireActeurId, tenantId)
                .stream().map(VehiculeDto.VehiculeResponse::fromEntity).collect(Collectors.toList());
    }
}
