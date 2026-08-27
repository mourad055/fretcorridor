package com.flysoft.fretcorridor.ida.service;

import com.flysoft.fretcorridor.ida.dto.KycAdminDto;
import com.flysoft.fretcorridor.ida.dto.KycDto;
import com.flysoft.fretcorridor.ida.entity.Acteur;
import com.flysoft.fretcorridor.ida.entity.RoleActeur;
import com.flysoft.fretcorridor.ida.repository.ActeurRepository;
import com.flysoft.fretcorridor.ida.repository.PieceJustificativeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Revue KYC par un Admin : lister les dossiers en attente, consulter le détail
 * et valider/rejeter les pièces des acteurs transport (CHAUFFEUR, TRANSPORTEUR,
 * CHAUFFEUR_PROPRIETAIRE). Complément des endpoints mobiles /api/kyc/*.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KycAdminService {

    private static final Set<RoleActeur> ROLES_KYC_ELIGIBLES = Set.of(
            RoleActeur.CHAUFFEUR,
            RoleActeur.TRANSPORTEUR,
            RoleActeur.CHAUFFEUR_PROPRIETAIRE);

    private final ActeurRepository acteurRepository;
    private final PieceJustificativeRepository pieceJustificativeRepository;
    private final DocumentStorageService documentStorageService;

    /**
     * File admin : profil mobile complet (NIVEAU_1) avec au moins une pièce déposée,
     * en attente de décision VALIDE → N2 ou REJETE.
     */
    @Transactional(readOnly = true)
    public List<KycAdminDto.ActeurSummary> listerEnAttente(String tenantId) {
        return acteurRepository.findByTenantId(tenantId).stream()
                .filter(this::estActeurKycEligible)
                .filter(a -> a.getNiveauKyc() == Acteur.NiveauKyc.NIVEAU_1)
                .filter(a -> possedePieces(a.getId()))
                .map(KycAdminDto.ActeurSummary::from)
                .toList();
    }

    /**
     * NIVEAU_2 : dossiers validés par l'admin (RG-011 — pièces vérifiées).
     * NIVEAU_1 : profils mobile N1 sans pièce — hors file admin (identité seule).
     */
    @Transactional(readOnly = true)
    public List<KycAdminDto.ActeurSummary> listerParNiveau(String tenantId, Acteur.NiveauKyc niveau) {
        if (niveau != Acteur.NiveauKyc.NIVEAU_1 && niveau != Acteur.NiveauKyc.NIVEAU_2) {
            throw new RuntimeException("NIVEAU_KYC_INVALIDE");
        }
        return acteurRepository.findByTenantIdAndNiveauKyc(tenantId, niveau).stream()
                .filter(this::estActeurKycEligible)
                .filter(a -> niveau != Acteur.NiveauKyc.NIVEAU_1 || !possedePieces(a.getId()))
                .map(KycAdminDto.ActeurSummary::from)
                .toList();
    }

    private boolean possedePieces(UUID acteurId) {
        return !pieceJustificativeRepository.findByActeurId(acteurId).isEmpty();
    }

    @Transactional(readOnly = true)
    public KycAdminDto.ActeurDetail getDetail(UUID acteurId, String tenantId) {
        Acteur acteur = acteurKycDuTenant(acteurId, tenantId);
        return KycAdminDto.ActeurDetail.from(acteur, piecesDe(acteur));
    }

    @Transactional(readOnly = true)
    public DocumentStorageService.ContenuObjet lirePiece(UUID acteurId, UUID pieceId, String tenantId) {
        Acteur acteur = acteurKycDuTenant(acteurId, tenantId);
        var piece = pieceJustificativeRepository.findByIdAndActeurId(pieceId, acteur.getId())
                .orElseThrow(() -> new RuntimeException("KYC_PIECE_INTROUVABLE"));
        return documentStorageService.lireContenu(piece.getObjectKey());
    }

    @Transactional
    public KycAdminDto.ActeurSummary prendreDecision(
            UUID acteurId, String tenantId, KycAdminDto.DecisionRequest request) {

        Acteur acteur = acteurKycDuTenant(acteurId, tenantId);
        KycAdminDto.Decision decision = parserDecision(request.getDecision());

        switch (decision) {
            case VALIDE -> acteur.setNiveauKyc(Acteur.NiveauKyc.NIVEAU_2);
            case REJETE -> {
                boolean possedePieces = !pieceJustificativeRepository.findByActeurId(acteur.getId()).isEmpty();
                acteur.setNiveauKyc(possedePieces
                        ? Acteur.NiveauKyc.NIVEAU_1
                        : Acteur.NiveauKyc.NIVEAU_0);
                if (request.getMotif() != null && !request.getMotif().isBlank()) {
                    log.info("KYC rejeté pour acteur {} (tenant {}) : {}",
                            acteurId, tenantId, request.getMotif());
                }
            }
        }

        acteur = acteurRepository.save(acteur);
        return KycAdminDto.ActeurSummary.from(acteur);
    }

    private KycAdminDto.Decision parserDecision(String decision) {
        try {
            return KycAdminDto.Decision.valueOf(decision);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("DECISION_INVALIDE");
        }
    }

    private boolean estActeurKycEligible(Acteur acteur) {
        return acteur.getRoles().stream().anyMatch(ROLES_KYC_ELIGIBLES::contains);
    }

    // Même principe IDOR que GestionCompteService : acteur hors tenant = introuvable.
    private Acteur acteurKycDuTenant(UUID acteurId, String tenantId) {
        Acteur acteur = acteurRepository.findById(acteurId)
                .orElseThrow(() -> new RuntimeException("KYC_ACTEUR_INTROUVABLE"));
        if (!acteur.getTenantId().equals(tenantId)) {
            throw new RuntimeException("KYC_ACTEUR_INTROUVABLE");
        }
        if (!estActeurKycEligible(acteur)) {
            throw new RuntimeException("KYC_ACTEUR_INTROUVABLE");
        }
        return acteur;
    }

    private List<KycDto.PieceResponse> piecesDe(Acteur acteur) {
        return pieceJustificativeRepository.findByActeurId(acteur.getId()).stream()
                .map(p -> KycDto.PieceResponse.builder()
                        .id(p.getId())
                        .typeDocument(p.getTypeDocument())
                        .url(documentStorageService.urlAcces(p.getObjectKey()))
                        .dateDepot(p.getDateDepot())
                        .build())
                .toList();
    }
}
