package com.flysoft.fretcorridor.ida.service;

import com.flysoft.fretcorridor.ida.dto.KycDto;
import com.flysoft.fretcorridor.ida.entity.Acteur;
import com.flysoft.fretcorridor.ida.entity.Organisation;
import com.flysoft.fretcorridor.ida.repository.ActeurRepository;
import com.flysoft.fretcorridor.ida.repository.OrganisationRepository;
import com.flysoft.fretcorridor.ida.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KycService {

    private final ActeurRepository acteurRepository;
    private final OrganisationRepository organisationRepository;
    private final JwtService jwtService;

    @Transactional
    public KycDto.CompletionResponse completerProfilParticulier(
            UUID acteurId, KycDto.CompleterProfilParticulierRequest request) {

        Acteur acteur = acteurRepository.findById(acteurId)
                .orElseThrow(() -> new RuntimeException("ACTEUR_INTROUVABLE"));

        acteur.setNom(request.getNom());
        acteur.setPrenom(request.getPrenom());
        progresserVersNiveau1(acteur);

        acteur = acteurRepository.save(acteur);
        return construireReponse(acteur);
    }

    @Transactional
    public KycDto.CompletionResponse completerProfilEntreprise(
            UUID acteurId, KycDto.CompleterProfilEntrepriseRequest request) {

        Acteur acteur = acteurRepository.findById(acteurId)
                .orElseThrow(() -> new RuntimeException("ACTEUR_INTROUVABLE"));

        Organisation organisation = Organisation.builder()
                .raisonSociale(request.getRaisonSociale())
                .numeroRegistreCommerce(request.getNumeroRegistreCommerce())
                .tenantId(acteur.getTenantId())
                .build();
        organisation = organisationRepository.save(organisation);

        acteur.setOrganisation(organisation);
        acteur.setRaisonSociale(request.getRaisonSociale());
        progresserVersNiveau1(acteur);

        acteur = acteurRepository.save(acteur);
        return construireReponse(acteur);
    }

    @Transactional(readOnly = true)
    public KycDto.ProfilResponse getProfil(UUID acteurId) {
        Acteur acteur = acteurRepository.findById(acteurId)
                .orElseThrow(() -> new RuntimeException("ACTEUR_INTROUVABLE"));
        return KycDto.ProfilResponse.fromEntity(acteur);
    }

    // RG-011 : identité déclarée → niveau 1 (si l'acteur est encore au niveau 0)
    private void progresserVersNiveau1(Acteur acteur) {
        if (acteur.getNiveauKyc() == Acteur.NiveauKyc.NIVEAU_0) {
            acteur.setNiveauKyc(Acteur.NiveauKyc.NIVEAU_1);
        }
    }

    private KycDto.CompletionResponse construireReponse(Acteur acteur) {
        return KycDto.CompletionResponse.builder()
                .accessToken(jwtService.genererAccessToken(acteur))
                .refreshToken(jwtService.genererRefreshToken(acteur))
                .profil(KycDto.ProfilResponse.fromEntity(acteur))
                .build();
    }
}
