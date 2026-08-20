package com.flysoft.fretcorridor.ida.service;

import com.flysoft.fretcorridor.ida.dto.KycDto;
import com.flysoft.fretcorridor.ida.entity.Acteur;
import com.flysoft.fretcorridor.ida.entity.Organisation;
import com.flysoft.fretcorridor.ida.entity.PieceJustificative;
import com.flysoft.fretcorridor.ida.repository.ActeurRepository;
import com.flysoft.fretcorridor.ida.repository.OrganisationRepository;
import com.flysoft.fretcorridor.ida.repository.PieceJustificativeRepository;
import com.flysoft.fretcorridor.ida.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KycService {

    private final ActeurRepository acteurRepository;
    private final OrganisationRepository organisationRepository;
    private final PieceJustificativeRepository pieceJustificativeRepository;
    private final DocumentStorageService documentStorageService;
    private final JwtService jwtService;

    @Transactional
    public KycDto.CompletionResponse completerProfilParticulier(
            UUID acteurId, KycDto.CompleterProfilParticulierRequest request) {

        Acteur acteur = acteurRepository.findById(acteurId)
                .orElseThrow(() -> new RuntimeException("ACTEUR_INTROUVABLE"));

        acteur.setNom(request.getNom());
        acteur.setPrenom(request.getPrenom());
        evaluerProgressionNiveau1(acteur);

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
        evaluerProgressionNiveau1(acteur);

        acteur = acteurRepository.save(acteur);
        return construireReponse(acteur);
    }

    @Transactional(readOnly = true)
    public KycDto.ProfilResponse getProfil(UUID acteurId) {
        Acteur acteur = acteurRepository.findById(acteurId)
                .orElseThrow(() -> new RuntimeException("ACTEUR_INTROUVABLE"));
        return KycDto.ProfilResponse.fromEntity(acteur, piecesDe(acteur));
    }

    // ── DÉPOSER UNE PIÈCE JUSTIFICATIVE (EF-IDA-03) ───────────
    @Transactional
    public KycDto.CompletionResponse deposerPiece(
            UUID acteurId, String typeDocument, MultipartFile fichier) {

        if (fichier == null || fichier.isEmpty()) {
            throw new RuntimeException("FICHIER_VIDE");
        }

        Acteur acteur = acteurRepository.findById(acteurId)
                .orElseThrow(() -> new RuntimeException("ACTEUR_INTROUVABLE"));

        String objectKey = documentStorageService.deposer(acteur.getTenantId(), acteurId, typeDocument, fichier);

        pieceJustificativeRepository.save(PieceJustificative.builder()
                .acteur(acteur)
                .typeDocument(typeDocument)
                .objectKey(objectKey)
                .build());

        evaluerProgressionNiveau1(acteur);
        acteur = acteurRepository.save(acteur);

        return construireReponse(acteur);
    }

    // RG-011 : niveau 1 = identité déclarée ET au moins une pièce déposée —
    // les deux conditions peuvent être remplies dans n'importe quel ordre,
    // donc réévaluées à chaque étape plutôt que déclenchées une seule fois.
    private void evaluerProgressionNiveau1(Acteur acteur) {
        if (acteur.getNiveauKyc() != Acteur.NiveauKyc.NIVEAU_0) {
            return;
        }
        boolean identiteDeclaree = (acteur.getNom() != null && acteur.getPrenom() != null)
                || acteur.getRaisonSociale() != null;
        boolean pieceDeposee = !pieceJustificativeRepository.findByActeurId(acteur.getId()).isEmpty();
        if (identiteDeclaree && pieceDeposee) {
            acteur.setNiveauKyc(Acteur.NiveauKyc.NIVEAU_1);
        }
    }

    private List<KycDto.PieceResponse> piecesDe(Acteur acteur) {
        return pieceJustificativeRepository.findByActeurId(acteur.getId()).stream()
                .map(p -> KycDto.PieceResponse.builder()
                        .typeDocument(p.getTypeDocument())
                        .url(documentStorageService.urlAcces(p.getObjectKey()))
                        .dateDepot(p.getDateDepot())
                        .build())
                .toList();
    }

    private KycDto.CompletionResponse construireReponse(Acteur acteur) {
        return KycDto.CompletionResponse.builder()
                .accessToken(jwtService.genererAccessToken(acteur))
                .refreshToken(jwtService.genererRefreshToken(acteur))
                .profil(KycDto.ProfilResponse.fromEntity(acteur, piecesDe(acteur)))
                .build();
    }
}
