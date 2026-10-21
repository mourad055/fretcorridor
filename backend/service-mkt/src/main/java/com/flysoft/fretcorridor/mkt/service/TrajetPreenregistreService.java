package com.flysoft.fretcorridor.mkt.service;

import com.flysoft.fretcorridor.mkt.dto.TrajetPreenregistreDto;
import com.flysoft.fretcorridor.mkt.entity.CatalogueEmballage;
import com.flysoft.fretcorridor.mkt.entity.Demande;
import com.flysoft.fretcorridor.mkt.entity.TrajetPreenregistre;
import com.flysoft.fretcorridor.mkt.repository.CatalogueEmballageRepository;
import com.flysoft.fretcorridor.mkt.repository.TrajetPreenregistreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TrajetPreenregistreService {

    private final TrajetPreenregistreRepository trajetRepository;
    private final CatalogueEmballageRepository catalogueRepository;

    public TrajetPreenregistreService(TrajetPreenregistreRepository trajetRepository,
                                       CatalogueEmballageRepository catalogueRepository) {
        this.trajetRepository = trajetRepository;
        this.catalogueRepository = catalogueRepository;
    }

    @Transactional
    public TrajetPreenregistre enregistrer(TrajetPreenregistreDto.EnregistrerRequest request,
                                            UUID clientActeurId, String tenantId) {
        CatalogueEmballage emballage = catalogueRepository.findById(request.getTypeEmballageId())
                .orElseThrow(() -> new RuntimeException("TYPE_EMBALLAGE_INTROUVABLE"));

        TrajetPreenregistre trajet = TrajetPreenregistre.builder()
                .clientActeurId(clientActeurId)
                .tenantId(tenantId)
                .nom(request.getNom())
                .villeDepart(request.getVilleDepart())
                .villeArrivee(request.getVilleArrivee())
                .typeEmballage(emballage)
                .quantite(request.getQuantite())
                .fragile(request.getFragile())
                .perissable(request.getPerissable())
                .dangereuse(request.getDangereuse())
                .grandeValeur(request.getGrandeValeur())
                .categorieMarchandise(Demande.CategorieMarchandise.valueOf(request.getCategorieMarchandise()))
                .modeCollecte(Demande.ModeCollecte.valueOf(request.getModeCollecte()))
                .destinataireNom(request.getDestinataireNom())
                .destinataireTelephone(request.getDestinataireTelephone())
                .build();

        return trajetRepository.save(trajet);
    }

    @Transactional(readOnly = true)
    public List<TrajetPreenregistre> listerMesTrajets(UUID clientActeurId, String tenantId) {
        return trajetRepository.findByClientActeurIdAndTenantIdOrderByDateCreationDesc(clientActeurId, tenantId);
    }

    @Transactional
    public void supprimer(UUID id, UUID clientActeurId, String tenantId) {
        TrajetPreenregistre trajet = trajetRepository.findByIdAndClientActeurIdAndTenantId(id, clientActeurId, tenantId)
                .orElseThrow(() -> new RuntimeException("TRAJET_INTROUVABLE"));
        trajetRepository.delete(trajet);
    }
}
