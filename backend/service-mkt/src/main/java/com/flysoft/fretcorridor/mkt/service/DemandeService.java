package com.flysoft.fretcorridor.mkt.service;

import com.flysoft.fretcorridor.mkt.dto.DemandeDto;
import com.flysoft.fretcorridor.mkt.entity.CatalogueEmballage;
import com.flysoft.fretcorridor.mkt.entity.Demande;
import com.flysoft.fretcorridor.mkt.repository.CatalogueEmballageRepository;
import com.flysoft.fretcorridor.mkt.repository.DemandeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemandeService {

    // Coefficient volumétrique MVP (kg/m³) — placeholder simple, la vraie
    // tarification (RG-037) relève du moteur, pas de ce service.
    private static final double COEFFICIENT_VOLUMETRIQUE = 200.0;

    private final DemandeRepository demandeRepository;
    private final CatalogueEmballageRepository catalogueRepository;

    // RG-038 : publication exige le niveau KYC 1 minimum
    @Transactional
    public DemandeDto.DemandeResponse publier(
            DemandeDto.PublierRequest request, UUID clientActeurId, String tenantId, String niveauKyc) {

        if ("NIVEAU_0".equals(niveauKyc)) {
            throw new RuntimeException("KYC_INSUFFISANT_NIVEAU_1_REQUIS");
        }

        CatalogueEmballage emballage = catalogueRepository.findById(request.getTypeEmballageId())
                .orElseThrow(() -> new RuntimeException("TYPE_EMBALLAGE_INTROUVABLE"));

        double poidsTotal = emballage.getPoidsUnitaireKg() * request.getQuantite();
        double volumeTotal = emballage.getVolumeUnitaireM3() * request.getQuantite();
        double poidsTaxable = Math.max(poidsTotal, volumeTotal * COEFFICIENT_VOLUMETRIQUE);

        Demande demande = Demande.builder()
                .clientActeurId(clientActeurId)
                .villeDepart(request.getVilleDepart())
                .villeArrivee(request.getVilleArrivee())
                .typeEmballage(emballage)
                .quantite(request.getQuantite())
                .poidsTotalKg(poidsTotal)
                .volumeTotalM3(volumeTotal)
                .poidsTaxableKg(poidsTaxable)
                .fragile(request.getFragile() != null ? request.getFragile() : emballage.getFragileParDefaut())
                .perissable(request.getPerissable())
                .dangereuse(request.getDangereuse())
                .grandeValeur(request.getGrandeValeur())
                .typeDisponibilite(Demande.TypeDisponibilite.valueOf(request.getTypeDisponibilite()))
                .dateDisponibilite(request.getDateDisponibilite())
                .modeCollecte(Demande.ModeCollecte.valueOf(request.getModeCollecte()))
                .destinataireNom(request.getDestinataireNom())
                .destinataireTelephone(request.getDestinataireTelephone())
                .tenantId(tenantId)
                .build();

        demande = demandeRepository.save(demande);
        return DemandeDto.DemandeResponse.fromEntity(demande);
    }

    @Transactional(readOnly = true)
    public List<DemandeDto.DemandeResponse> getMesDemandes(UUID clientActeurId, String tenantId) {
        return demandeRepository.findByClientActeurIdAndTenantIdOrderByDateCreationDesc(clientActeurId, tenantId)
                .stream().map(DemandeDto.DemandeResponse::fromEntity).toList();
    }

    // S5 — stub en attendant service-mat/service-opt (Moteur).
    // Retourne toujours une liste vide pour l'instant ; à remplacer par un
    // vrai appel au moteur de matching dès qu'il sera prêt (voir README).
    @Transactional(readOnly = true)
    public List<DemandeDto.PropositionResponse> getPropositions(UUID demandeId, String tenantId) {
        demandeRepository.findByIdAndTenantId(demandeId, tenantId)
                .orElseThrow(() -> new RuntimeException("DEMANDE_INTROUVABLE"));
        return List.of();
    }
}
