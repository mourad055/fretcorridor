package com.flysoft.fretcorridor.flt.service;

import com.flysoft.fretcorridor.flt.dto.VehiculeDto;
import com.flysoft.fretcorridor.flt.entity.Vehicule;
import com.flysoft.fretcorridor.flt.repository.VehiculeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
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

    // CRUD véhicule (retour utilisatrice 21/08) : modifier/supprimer un
    // véhicule déjà déclaré. Même garde IDOR que consulter() ci-dessus --
    // "introuvable" pour "n'existe pas" et "pas le vôtre", jamais de 403 qui
    // confirmerait l'existence d'un véhicule d'un autre acteur/tenant.
    @Transactional
    public VehiculeDto.VehiculeResponse modifier(UUID vehiculeId, UUID proprietaireActeurId, String tenantId,
                                                   VehiculeDto.DeclarerRequest request) {
        Vehicule vehicule = trouverAppartenant(vehiculeId, proprietaireActeurId, tenantId);
        vehicule.setTypeVehicule(request.getTypeVehicule());
        vehicule.setImmatriculation(request.getImmatriculation());
        vehicule.setProfilHauteurMetres(request.getProfilHauteurMetres());
        vehicule.setProfilLargeurMetres(request.getProfilLargeurMetres());
        vehicule.setProfilLongueurMetres(request.getProfilLongueurMetres());
        vehicule.setProfilPoidsMaxTonnes(request.getProfilPoidsMaxTonnes());
        vehicule.setProfilChargeMaxParEssieuTonnes(request.getProfilChargeMaxParEssieuTonnes());
        vehicule.setProfilNombreEssieux(request.getProfilNombreEssieux());
        vehicule.setProfilMatieresDangereuses(request.isProfilMatieresDangereuses());
        try {
            vehicule = vehiculeRepository.save(vehicule);
        } catch (DataIntegrityViolationException doublon) {
            throw new ImmatriculationDejaUtiliseeException(request.getImmatriculation());
        }
        return VehiculeDto.VehiculeResponse.fromEntity(vehicule);
    }

    @Transactional
    public void supprimer(UUID vehiculeId, UUID proprietaireActeurId, String tenantId) {
        Vehicule vehicule = trouverAppartenant(vehiculeId, proprietaireActeurId, tenantId);
        vehiculeRepository.delete(vehicule);
    }

    private Vehicule trouverAppartenant(UUID vehiculeId, UUID proprietaireActeurId, String tenantId) {
        return vehiculeRepository.findById(vehiculeId)
                .filter(v -> proprietaireActeurId.equals(v.getProprietaireActeurId()) && tenantId.equals(v.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicule introuvable : " + vehiculeId));
    }
}
